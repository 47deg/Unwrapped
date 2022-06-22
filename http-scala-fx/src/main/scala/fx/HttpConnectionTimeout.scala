package fx

/**
 * Models http connection timeouts as finite durations
 */
opaque type HttpConnectionTimeout = Long

extension (h: HttpConnectionTimeout)
  def toLong:Long = h

object HttpConnectionTimeout:
  /**
   * @constructor
   */
  inline def apply(durationInSeconds: Long): HttpConnectionTimeout =
    requires(durationInSeconds > 0, "Durations must be positive", durationInSeconds)

  def of(durationInSeconds: Long): Errors[String] ?=> HttpConnectionTimeout =
    if (durationInSeconds > 0)
      durationInSeconds
    else
      "Durations must be positive".shift
  
  /**
   * Default connection timeout is 30 seconds
   */
  given defaultHttpConnectionTimeout: HttpConnectionTimeout =
    HttpConnectionTimeout(30)
