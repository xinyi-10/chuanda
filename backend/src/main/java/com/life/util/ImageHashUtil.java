package com.life.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * 图片指纹工具。
 *
 * <ul>
 *   <li><b>MD5</b>：内容精确判重 —— 同一文件重复上传直接拦截。</li>
 *   <li><b>aHash</b>（平均哈希）：感知哈希 —— 缩放、压缩、轻微改动后仍能识别为「视觉近似」。</li>
 * </ul>
 *
 * <p>aHash 相比 DCT pHash 实现更简单，对常见图片变换已足够鲁棒；
 * 若需要更强鲁棒性可在不改调用方的前提下替换为 pHash / dHash。</p>
 */
public final class ImageHashUtil {

    private static final int HASH_SIDE = 8;

    private ImageHashUtil() {
    }

    /** 计算文件内容 MD5（32 位小写十六进制） */
    public static String md5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
        }
        return toHex(md.digest());
    }

    /** 计算字节内容 MD5（对象存储场景：不落本地磁盘） */
    public static String md5(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        return toHex(md.digest());
    }

    /**
     * 感知哈希（aHash）：缩放为 8x8 灰度 → 与均值比较 → 64 位指纹（16 位十六进制）。
     * 非图片文件返回 null。
     */
    public static String aHash(File file) {
        try {
            return aHashOf(ImageIO.read(file));
        } catch (Exception e) {
            return null;
        }
    }

    /** 感知哈希（字节版本，适配对象存储场景） */
    public static String aHash(byte[] data) {
        try {
            return aHashOf(ImageIO.read(new ByteArrayInputStream(data)));
        } catch (Exception e) {
            return null;
        }
    }

    private static String aHashOf(BufferedImage src) {
        if (src == null) {
            return null;
        }
        BufferedImage small = new BufferedImage(HASH_SIDE, HASH_SIDE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = small.createGraphics();
        g.drawImage(src, 0, 0, HASH_SIDE, HASH_SIDE, null);
        g.dispose();

        int[] px = new int[HASH_SIDE * HASH_SIDE];
        long sum = 0;
        for (int y = 0; y < HASH_SIDE; y++) {
            for (int x = 0; x < HASH_SIDE; x++) {
                int v = small.getRaster().getSample(x, y, 0);
                px[y * HASH_SIDE + x] = v;
                sum += v;
            }
        }
        double avg = sum / (double) px.length;

        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < px.length; i += 4) {
            int nibble = 0;
            for (int j = 0; j < 4; j++) {
                nibble <<= 1;
                if (px[i + j] > avg) {
                    nibble |= 1;
                }
            }
            sb.append(Integer.toHexString(nibble));
        }
        return sb.toString();
    }

    /** 两个十六进制指纹的汉明距离（不同位个数）；任一为空或长度不符视为不相似（返回 64） */
    public static int hammingDistance(String h1, String h2) {
        if (h1 == null || h2 == null || h1.length() != h2.length()) {
            return 64;
        }
        int dist = 0;
        for (int i = 0; i < h1.length(); i++) {
            int a = Character.digit(h1.charAt(i), 16);
            int b = Character.digit(h2.charAt(i), 16);
            if (a < 0 || b < 0) {
                return 64;
            }
            dist += Integer.bitCount(a ^ b);
        }
        return dist;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
