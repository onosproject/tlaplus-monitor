package tlc2.overrides.source;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Sources.
 */
public class Sources {
    public static Source getSource(String source) {
        try {
            URI sourceUri = new URI(source);
            switch (sourceUri.getScheme()) {
                case "kafka":
                    String path = sourceUri.getPath().substring(1);
                    if (path.equals("")) {
                        throw new IllegalStateException("No topic specified");
                    }
                    return new KafkaSource(sourceUri.getHost(), sourceUri.getPort(), path);
                default:
                    throw new IllegalStateException("Unknown consumer scheme");
            }
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
