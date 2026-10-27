import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.JarFile;

/** Real artifact checks; no substitute instrumentation or class loader. */
public final class GraphIdentityTest {
    public static void main(String[] args) throws Exception {
        if (!args[0].equals("active")) {
            boolean rejected = false;
            try {
                FlatGraphProbe.identity();
            } catch (IllegalStateException expected) {
                rejected = true;
            }
            CapabilityProbe.require(rejected, "Inactive or loose-class identity must reject");
        } else {
            Path jar = Path.of(args[1]).toRealPath();
            var observed = FlatGraphProbe.identity();
            var digest = MessageDigest.getInstance("SHA-256");
            var hex = HexFormat.of();
            CapabilityProbe.require(observed.get("jar-sha256").equals(
                hex.formatHex(digest.digest(Files.readAllBytes(jar)))), "JAR hash mismatch");
            try (var archive = new JarFile(jar.toFile());
                 var stream = archive.getInputStream(archive.getJarEntry("FlatGraphProbe.class"))) {
                CapabilityProbe.require(observed.get("class-sha256").equals(
                    hex.formatHex(digest.digest(stream.readAllBytes()))), "Class hash mismatch");
            }
            CapabilityProbe.require(Path.of(java.net.URI.create(observed.get("location")))
                .toRealPath().equals(jar), "Loaded location mismatch");
            CapabilityProbe.require(observed.equals(FlatGraphProbe.identity()), "Identity changed");
        }
        System.out.printf("{:graph-identity :%s :status :passed}%n", args[0]);
    }
}
