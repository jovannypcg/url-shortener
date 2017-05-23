package mx.jovannypcg.urlshortener.controllers;

import mx.jovannypcg.urlshortener.dao.ShortLinkRepository;
import mx.jovannypcg.urlshortener.exceptions.DestinationAlreadyExistsException;
import mx.jovannypcg.urlshortener.exceptions.DestinationNotFoundException;
import mx.jovannypcg.urlshortener.model.ShortLink;
import mx.jovannypcg.urlshortener.model.ShortLinkRequest;
import mx.jovannypcg.urlshortener.util.Base62;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Contains the actions to deal with short links.
 *
 * @author  Jovanny Pablo Cruz Gomez
 *          Software Engineer
 *          jovannypcg@yahoo.com
 */
@RestController
public class ShortLinksController {
    @Autowired
    ShortLinkRepository shortLinkRepository;

    /**
     * Creates a new short link.
     * If the destination in <code>request</code> is already stored in database, it is
     * not created another record. Instead, the existing record is retrieved and returned.
     *
     * @param request JSON request that includes the <code>destination</code> URL.
     * @return The created ShortLink in JSON format.
     */
    @RequestMapping(value = "/v1/shortlinks", method = RequestMethod.POST)
    public ShortLink createShortLink(@RequestBody ShortLinkRequest request) {
        int lastIdInserted = (int) shortLinkRepository.count();

        Optional<ShortLink> existingDestination = shortLinkRepository.findByDestination(request.getDestination());

        if (existingDestination.isPresent()) {
            return existingDestination.get();
        }

        ShortLink newShortLink = new ShortLink();
        newShortLink.setDestination(request.getDestination());
        newShortLink.setSlug(Base62.encode(lastIdInserted  + 1));

        return shortLinkRepository.save(newShortLink);
    }

    @RequestMapping(value = "/v1/shortlinks", method = RequestMethod.GET)
    public List<ShortLink> getShortLinks() {
        List<ShortLink> links = new ArrayList<>();

        shortLinkRepository.findAll().forEach(links::add);
        return links;
    }

    @RequestMapping(value = "/{slug}", method = RequestMethod.GET)
    public ShortLink getDestinationFrom(@PathVariable String slug) {
        int destinationId = Base62.decode(slug);

        ShortLink retrievedShortLink = shortLinkRepository.findOne(destinationId);

        if (retrievedShortLink == null) {
            throw new DestinationNotFoundException(slug);
        }

        return retrievedShortLink;
    }

    /**
     * Validates the given destination does not exist in database.
     * If it exists, a <code>DestinationAlreadyExistsException</code> is thrown.
     *
     * @param destination Destination to validate.
     */
    private void validateDestination(String destination) {
        if (shortLinkRepository.findByDestination(destination).isPresent()) {
            throw new DestinationAlreadyExistsException(destination);
        }
    }

    @ExceptionHandler(DestinationAlreadyExistsException.class)
    void handleDestinationAlreadyExistsException(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value());
    }

    @ExceptionHandler(DestinationNotFoundException.class)
    void handleDestinationNotFoundException(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value());
    }
}
