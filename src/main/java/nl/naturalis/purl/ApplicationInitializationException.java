package nl.naturalis.purl;

/**
 * Exception thrown if anything goes wrong while configuring and initializing the PURL application.
 * 
 * @see Registry
 * 
 * @author Ayco Holleman
 *
 */
public class ApplicationInitializationException extends RuntimeException {

  /**
   * @param message
   */
  public ApplicationInitializationException(String message) {
    super(message);
  }

  /**
   * @param cause
   */
  public ApplicationInitializationException(Throwable cause) {
    super(cause);
  }

  /**
   * @param message
   * @param cause
   */
  public ApplicationInitializationException(String message, Throwable cause) {
    super(message, cause);
  }

}
