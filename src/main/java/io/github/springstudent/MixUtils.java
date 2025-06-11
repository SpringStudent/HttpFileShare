package io.github.springstudent;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
/**
 * @author ZhouNing
 * @date 2025/6/9 16:46
 **/
public class MixUtils {
    /**
     * 用于随机选的字符
     */
    public static final String BASE_STR = "abcdefghijklmnopqrstuvwxyz0123456789";

    public static String randomString(int length) {
        final StringBuilder sb = new StringBuilder(length);
        if (length < 1) {
            length = 1;
        }
        int baseLength = BASE_STR.length();
        for (int i = 0; i < length; i++) {
            int number = ThreadLocalRandom.current().nextInt(baseLength);
            sb.append(BASE_STR.charAt(number));
        }
        return sb.toString();
    }

    public static InetAddress getIp() throws Exception {
        for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (InetAddress addr : java.util.Collections.list(ni.getInetAddresses())) {
                if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                    return addr;
                }
            }
        }
        return InetAddress.getLocalHost();
    }

    public static Integer getPort() {
        Integer port = null;
        if (System.getProperty("httpFileShare.port") != null) {
            try {
                port = Integer.parseInt(System.getProperty("httpFileShare.port"));
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number in system property 'httpFileShare.port'");
            }
        }
        if (port != null) {
            return port;
        } else {
            try (ServerSocket socket = new ServerSocket(0)) {
                port = socket.getLocalPort();
            } catch (IOException e) {
                port = 54321;
                System.err.println("Failed to get a free port");
            }
            return port;
        }

    }

    public static BufferedImage qrCode(String url) throws WriterException {
        // 生成二维码图片
        int size = 300;
        BitMatrix bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, size, size);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return image;
    }

}
