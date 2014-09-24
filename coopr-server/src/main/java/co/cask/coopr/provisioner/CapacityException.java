package co.cask.coopr.provisioner;

/**
 * Thrown to indicate that there was a problem with provisioner capacity.
 */
public class CapacityException extends Exception {

  /**
   * New exception with error message.
   * @param message the error message
   */
  public CapacityException(String message) {
    super(message);
  }

  public CapacityException(String message, Throwable cause) {
    super(message, cause);
  }

  public CapacityException(Throwable cause) {
    super(cause);
  }
}
