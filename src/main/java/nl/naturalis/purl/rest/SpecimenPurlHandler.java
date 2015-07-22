package nl.naturalis.purl.rest;

import java.net.URI;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import nl.naturalis.nda.client.MultiMediaClient;
import nl.naturalis.nda.client.NBAResourceException;
import nl.naturalis.nda.domain.MultiMediaObject;
import nl.naturalis.nda.domain.ServiceAccessPoint;
import nl.naturalis.purl.PurlException;
import nl.naturalis.purl.Registry;

/**
 * A {@link PurlHandler} capable of handling PURLs for specimen objects.
 * 
 * @author Ayco Holleman
 * @created Jul 9, 2015
 *
 */
public class SpecimenPurlHandler extends AbstractPurlHandler {

	private MultiMediaObject[] multimedia;


	public SpecimenPurlHandler(HttpServletRequest request, UriInfo uriInfo)
	{
		super(request, uriInfo);
	}


	protected Response doHandle() throws Exception
	{
		if (!Registry.getInstance().getSpecimenClient().exists(objectID)) {
			return Response.status(Status.NOT_FOUND).build();
		}
		ContentNegotiator negotiator = ContentNegotiatorFactory.getInstance().forSpecimens(accept);
		MediaType mediaType;
		if (negotiator.clientAcceptsRepositoryMediaType()) {
			mediaType = negotiator.negotiate(getMultiMedia());
		}
		else {
			mediaType = negotiator.negotiate();
		}
		if (mediaType == null) {
			return Response.notAcceptable(negotiator.getAlternatives(getMultiMedia())).build();
		}
		return Response.temporaryRedirect(getLocation(mediaType)).build();
	}


	private URI getLocation(MediaType mediaType) throws NBAResourceException, PurlException
	{
		if (mediaType.isCompatible(MediaType.TEXT_HTML_TYPE)) {
			return getBioportalUri();
		}
		if (mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
			return getNbaUri();
		}
		return getMedialibUri(mediaType);
	}


	private URI getBioportalUri()
	{
		StringBuilder url = new StringBuilder(128);
		url.append(Registry.getInstance().getBioportalBaseUrl());
		url.append("/nba/result?nba_request=");
		url.append(urlEncode("specimen/get-specimen/?unitID="));
		url.append(urlEncode(objectID));
		return URI.create(url.toString());
	}


	private URI getNbaUri()
	{
		StringBuilder url = new StringBuilder(128);
		url.append(Registry.getInstance().getNbaBaseUrl());
		url.append("/specimen/find/");
		url.append(urlEncode(objectID));
		return URI.create(url.toString());
	}


	private URI getMedialibUri(MediaType mediaType) throws NBAResourceException
	{
		MultiMediaObject[] multimedia = getMultiMedia();
		if (multimedia != null) {
			Set<ServiceAccessPoint.Variant> variants;
			for (MultiMediaObject mmo : multimedia) {
				if (mmo.getServiceAccessPoints() != null) {
					variants = mmo.getServiceAccessPoints().keySet();
					for (ServiceAccessPoint.Variant variant : variants) {
						ServiceAccessPoint sap = mmo.getServiceAccessPoints().get(variant);
						MediaType sapMediaType = MediaType.valueOf(sap.getFormat());
						if (sapMediaType.equals(mediaType)) {
							return sap.getAccessUri();
						}
					}
				}
			}
		}
		/*
		 * Because the content negotiator has iterated through the exact same
		 * multimedia objects to see which media types are available for the
		 * requested object, and if one of them matches what the client wants,
		 * we should never get here.
		 */
		assert (false);
		return null;
	}


	private MultiMediaObject[] getMultiMedia() throws NBAResourceException
	{
		if (multimedia == null) {
			MultiMediaClient client = Registry.getInstance().getMultiMediaClient();
			multimedia = client.getMultiMediaForSpecimen(objectID);
		}
		return multimedia;
	}

}
