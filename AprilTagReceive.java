import java.io.*;
import java.net.*;
// import InetAddress;

// sudo lsof -i :7709

// kill -9 $(lsof -i:7709 -t) 2> /dev/null && java AprilTagReceive.java

public class AprilTagReceive {
    public static int[] resolution = { 2688, 1242 };
    public static int[] sens = { 400, 83 };
    public static String fileName = "stream.txt";
    public static int counter = 0;
    public static final int skip_every = 5;

    public static void main(String args[]) throws IOException {
        // DatagramSocket sock = new DatagramSocket(7709, new InetAddress("name",0,1));

        DatagramSocket sock = new DatagramSocket(7709);
        byte buf[] = new byte[65536];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        while (true) {
            // if (counter < skip_every) {
            // counter++;
            // continue;
            // }
            // counter = 0;
            sock.receive(packet);

            DataInputStream ins = new DataInputStream(new ByteArrayInputStream(buf));
            int MAGIC1 = ins.readInt();
            int MAGIC2 = ins.readInt();
            assert (MAGIC1 == 0x41505249);
            assert (MAGIC2 == 0x4c544147);

            int version = ins.readInt();
            assert (version == 0x00010001);

            int ndets = ins.readInt();
            long utime = (((long) ins.readInt()) << 32) + ins.readInt();

            // System.out.printf("%13.6f: %d tags\n", utime / 1000000.0, ndets);
            String str = "{\"time\":" + utime + ", \"tags_amount\": " + ndets + ",\"tags\":[";

            for (int i = 0; i < ndets; i++) {
                int id = ins.readInt();
                int hamming = ins.readInt();
                float goodness = ins.readFloat();

                double c[] = new double[2];
                for (int j = 0; j < 2; j++)
                    c[j] = ins.readFloat();

                double p[][] = new double[4][2];
                for (int j = 0; j < 4; j++) {
                    p[j][0] = ins.readFloat();
                    p[j][1] = ins.readFloat();

                }

                double H[] = new double[9];
                for (int j = 0; j < 9; j++)
                    H[j] = ins.readFloat();
                str += "{\"id\": " + id + ",\"center\":" + tojsn(tocor(c), 0) + ", \"corners\":" + tojsn(tocor(p), 0);
                if (i != ndets) {
                    str += ",";
                }
                // String Hstring = "";
                // for (int a = 0; a < H.length; a++) {
                // Hstring += H[a] + " - ";
                // }
                // System.out.println(Hstring);

            }
            if (ndets == 0) {
                str += "{}";
            }
            str += "]}";
            System.out.println(str);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

                writer.write(str);
                writer.close();
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static double[][] tocor(double[][] p) {
        double result[][] = new double[4][2];
        for (int i = 0; i < p.length; i++) {
            for (int j = 0; j < p[0].length; j++) {
                result[i][j] = -p[i][j] + resolution[j] / 2 - sens[j];
            }
        }
        return result;
    }

    public static double[] tocor(double[] p) {
        double result[] = new double[p.length];
        for (int i = 0; i < p.length; i++) {
            result[i] = -p[i] + resolution[i] / 2 - sens[i];
        }
        return result;
    }

    public static String tojsn(double[][] k, int round) {
        String result = "";
        String p[][] = reround(k, round);

        result = "{\"ul\":{\"x\":" + p[3][1] + ",\"y\":" + p[3][0]
                + "}, \"ur\":{\"x\":" + p[2][1] + ",\"y\":" + p[2][0]
                + "},\"bl\":{\"x\":" + p[0][1] + ",\"y\":" + p[0][0]
                + "},\"br\":{\"x\":" + p[1][1] + ",\"y\":" + p[1][0]
                + "}}}";
        return result;
    }

    // id= 0, err=0 : {c_x:(149) ,c_y
    // :(-450)}{"ul":{"x"57,"y":-355{"ur":{"x"240,"y":-358{"bl":{"x"59,"y":-541{"bl":{"x"240,"y":-545)}
    public static String tojsn(double[] k, int round) {
        String result = "";
        String p[] = reround(k, round);

        result = "{\"x\":" + p[1] + " ,\"y\":" + p[0] + "}";
        return result;
    }

    public static String[][] reround(double[][] f, int n) {
        String result[][] = new String[f.length][f[0].length];
        for (int i = 0; i < f.length; i++) {
            for (int j = 0; j < f[0].length; j++) {
                result[i][j] = String.format("%." + n + "f", f[i][j]);
            }
        }
        return result;
    }

    public static String[] reround(double[] f, int n) {
        String result[] = new String[f.length];
        for (int i = 0; i < f.length; i++) {

            result[i] = String.format("%." + n + "f", f[i]);
        }
        return result;
    }
}