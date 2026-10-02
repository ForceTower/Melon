import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

class CompareScreenshots {
    record Result(long changed, long total, BufferedImage difference) {
        double ratio() { return (double) changed / total; }
    }

    static Result compare(BufferedImage expected, BufferedImage actual) {
        if (expected.getWidth() != actual.getWidth() || expected.getHeight() != actual.getHeight()) {
            throw new IllegalArgumentException("Screenshot dimensions differ; compare matching device configurations");
        }
        int width = expected.getWidth();
        int height = expected.getHeight();
        var difference = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        long changed = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int before = expected.getRGB(x, y);
                int after = actual.getRGB(x, y);
                if (before != after) changed++;
                difference.setRGB(x, y, before == after ? 0x22000000 : 0xffff00ff);
            }
        }
        return new Result(changed, (long) width * height, difference);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("--self-test")) {
            var expected = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
            var actual = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
            if (compare(expected, actual).changed() != 0) throw new AssertionError("Identical pixels");
            actual.setRGB(1, 0, 0xffffffff);
            var result = compare(expected, actual);
            if (result.changed() != 1 || result.ratio() != 0.25 || result.difference().getRGB(1, 0) != 0xffff00ff) {
                throw new AssertionError("One changed pixel must be detected and marked");
            }
            try {
                compare(expected, new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB));
                throw new AssertionError("Dimension mismatch must fail");
            } catch (IllegalArgumentException expectedFailure) { }
            System.out.println("Screenshot comparator: identical, changed pixel and dimensions passed");
            return;
        }
        if (args.length < 3 || args.length > 4) {
            throw new IllegalArgumentException("Usage: java scripts/CompareScreenshots.java EXPECTED.png ACTUAL.png DIFF.png [MAX_CHANGED_RATIO]");
        }
        double threshold = args.length == 4 ? Double.parseDouble(args[3]) : 0;
        if (!Double.isFinite(threshold) || threshold < 0 || threshold > 1) {
            throw new IllegalArgumentException("MAX_CHANGED_RATIO must be in [0,1]");
        }
        var expected = ImageIO.read(new File(args[0]));
        var actual = ImageIO.read(new File(args[1]));
        if (expected == null || actual == null) throw new IllegalArgumentException("Unsupported image");
        var result = compare(expected, actual);
        ImageIO.write(result.difference(), "png", new File(args[2]));
        boolean passed = result.ratio() <= threshold;
        System.out.printf(java.util.Locale.ROOT,
            "{\"passed\":%s,\"changedPixels\":%d,\"totalPixels\":%d,\"changedRatio\":%.8f,\"threshold\":%.8f}%n",
            passed, result.changed(), result.total(), result.ratio(), threshold);
        if (!passed) System.exit(1);
    }
}
