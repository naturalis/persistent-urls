package nl.naturalis.purl.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.naturalis.nba.api.InvalidQueryException;
import nl.naturalis.nba.api.QueryCondition;
import nl.naturalis.nba.api.QueryResult;
import nl.naturalis.nba.api.QueryResultItem;
import nl.naturalis.nba.api.QuerySpec;
import nl.naturalis.nba.api.model.MultiMediaObject;
import nl.naturalis.nba.api.model.ServiceAccessPoint;
import nl.naturalis.nba.api.model.Specimen;
import nl.naturalis.nba.client.MultiMediaObjectClient;
import nl.naturalis.purl.PurlException;
import nl.naturalis.purl.Registry;

/**
 * A {@code ContentNegotiator} establishes the type of content to be served to the client by comparing Accept headers with the actually
 * available content types for the requested object. N.B. content type is more formally known as media type.
 * 
 * @author Ayco Holleman
 *
 */
class ContentNegotiatorUtil {
  
  public static MediaType RDF_MEDIA_TYPE = MediaType.valueOf("application/rdf+xml");

  /**
   * Defined as {@code application/json;charset=UTF-8}. When sending a JSON response back to the client, use this media type in stead of the
   * prefab MediaType.APPLICATION_JSON. We seem to have reports that without the charset parameter, some browsers or browser versions don't
   * interpret the response as expected.
   */
  private static final String JSON_MEDIA_TYPE_STRING = "application/json;charset=UTF-8";
  /**
   * The {@link MediaType} corresponding to {@code JSON}.
   */
  static final MediaType JSON_MEDIA_TYPE = MediaType.valueOf(JSON_MEDIA_TYPE_STRING);

  private static final Logger logger = LogManager.getLogger(ContentNegotiatorUtil.class);
  private static final String JPEG = "image/jpeg";

  /**
   * Get multimedia for specified specimen.
   */
  static MultiMediaObject[] getMultiMedia(Specimen specimen) throws PurlException {
    logger.info("Retrieving multimedia for specimen with UnitID " + specimen.getUnitID());
    MultiMediaObjectClient client = Registry.getInstance().getMultiMediaClient();
    String field = "associatedSpecimenReference";
    String value = specimen.getId();
    QueryCondition condition = new QueryCondition(field, "=", value);
    QuerySpec query = new QuerySpec();
    query.setConstantScore(true);
    query.addCondition(condition);
    QueryResult<MultiMediaObject> result;
    try {
      result = client.query(query);
    } catch (InvalidQueryException e) {
      throw new PurlException(e);
    }
    MultiMediaObject[] multimedia = new MultiMediaObject[result.size()];
    int i = 0;
    for (QueryResultItem<MultiMediaObject> qri : result) {
      multimedia[i++] = qri.getItem();
    }
    logger.info("Number of multimedia found: " + multimedia.length);
    return multimedia;
  }

  /**
   * Retrieve Accept headers from the HTTP request and convert them to an array of {@code MediaType} instances. Note that clients can supply
   * multiple Accept headers, but they can also supply one Accept header with a comma-separated list of media types, or they could do both.
   * For debug purposes you can mimic Accept headers by adding an "__accept" query parameter to the PURL. If you do this, the actual Accept
   * headers (if any) will be ignored.
   * 
   * @param request
   * @return
   */
  static MediaType[] getRequestedMediaTypes(HttpServletRequest request) {
    String acceptParam = request.getParameter("__accept");
    if (acceptParam != null) {
      return getRequestedMediaTypesDebug(acceptParam);
    }
    List<MediaType> types = new ArrayList<>();
    Enumeration<String> acceptHeaders = request.getHeaders("Accept");
    while (acceptHeaders.hasMoreElements()) {
      String acceptHeader = acceptHeaders.nextElement();
      String[] mediaTypes = acceptHeader.split(",");
      for (String one : mediaTypes) {
        try {
          types.add(MediaType.valueOf(one));
        } catch (IllegalArgumentException e) {
          if (mediaTypes.length == 1) {
            logger.warn("Invalid Accept header in request: \"" + one + "\" (ignored)");
          } else {
            logger.warn("Invalid media type in Accept header: \"" + one + "\" (ignored)");
          }
        }
      }
    }
    return types.toArray(new MediaType[types.size()]);
  }

  /**
   * Extract the media types from the specified MultiMediaObject instances.
   */
  static Set<MediaType> getAvailableMultiMediaTypes(MultiMediaObject[] multimedia) {
    Set<MediaType> mediaTypes = new LinkedHashSet<>();
    for (MultiMediaObject mmo : multimedia) {
      if (mmo.getServiceAccessPoints() != null) {
        for (ServiceAccessPoint sap : mmo.getServiceAccessPoints()) {
          // TODO: HACK. Media type not always set. Solve in import!
          String format = sap.getFormat() == null ? JPEG : sap.getFormat();
          mediaTypes.add(MediaType.valueOf(format));
        }
      }
    }
    return mediaTypes;
  }

  static Set<MediaType> getAvailableMultiMediaTypes(Specimen specimen) {
    Set<MediaType> mediaTypes = new LinkedHashSet<>();
    if (specimen.getAssociatedMultiMediaUris() != null) {
      for (ServiceAccessPoint sap : specimen.getAssociatedMultiMediaUris()) {
        // TODO: HACK. Media type not always set. Solve in import!
        String format = sap.getFormat() == null ? JPEG : sap.getFormat();
        mediaTypes.add(MediaType.valueOf(format));
      }
    }
    return mediaTypes;
  }

  /**
   * Get URI of multimedia object that matches the specified media type.
   */
  static Optional<URI> findUriForMediaType(MediaType requested, MultiMediaObject[] multimedia) {
    for (MultiMediaObject mmo : multimedia) {
      if (mmo.getServiceAccessPoints() != null) {
        for (ServiceAccessPoint sap : mmo.getServiceAccessPoints()) {
          String format = sap.getFormat() == null ? JPEG : sap.getFormat();
          MediaType mt = MediaType.valueOf(format);
          if (requested.isCompatible(mt)) {
            return Optional.ofNullable(sap.getAccessUri());
          }
        }
      }
    }
    return Optional.empty();
  }

  static Optional<URI> findUriForMediaType(MediaType requested, Specimen specimen) {
    if (specimen.getAssociatedMultiMediaUris() != null) {
      for (ServiceAccessPoint sap : specimen.getAssociatedMultiMediaUris()) {
        String format = sap.getFormat() == null ? JPEG : sap.getFormat();
        MediaType mt = MediaType.valueOf(format);
        if (requested.isCompatible(mt)) {
          return Optional.ofNullable(sap.getAccessUri());
        }
      }
    }
    return Optional.empty();
  }

  private static MediaType[] getRequestedMediaTypesDebug(String requestParam) {
    String[] chunks = requestParam.split(",");
    List<MediaType> types = new ArrayList<>(chunks.length);
    for (String chunk : chunks) {
      try {
        types.add(MediaType.valueOf(chunk));
      } catch (IllegalArgumentException e) {
        logger.warn("Invalid media type in __accept parameter: \"" + chunk + "\" (ignored)");
      }
    }
    return types.toArray(new MediaType[types.size()]);
  }

}
