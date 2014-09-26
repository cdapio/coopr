package co.cask.coopr.http;

import co.cask.http.HttpResponder;
import com.google.common.base.Charsets;
import com.google.gson.Gson;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * Helper class for http handlers.
 */
public final class HttpHelper {
  private static final Logger LOG  = LoggerFactory.getLogger(HttpHelper.class);

  /**
   * Decode the request body into the given type of object using the give gson instance. In the case of an error,
   * this method will write to the responder and return a null. It is expected that the caller will check for a null
   * and return right away.
   *
   * @param request Request whose body should be decoded
   * @param responder Responder
   * @param type Type of object the body should be decoded into
   * @param gson Gson to use for decoding
   * @param <T> Type of object the body should be decoded into
   * @return Decoded request body, or null if there was an error.
   */
  public static <T> T decodeRequestBody(HttpRequest request, HttpResponder responder, Type type, Gson gson) {
    Reader reader = new InputStreamReader(new ChannelBufferInputStream(request.getContent()), Charsets.UTF_8);
    try {
      return gson.fromJson(reader, type);
    } catch (IllegalArgumentException e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, e.getMessage());
      return null;
    } catch (Exception e) {
      responder.sendError(HttpResponseStatus.BAD_REQUEST, "invalid request.");
      return null;
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOG.error("Exception closing reader for request body.", e);
      }
    }
  }
}
