package pl.robocikd.joboffers.domain.offer;

import pl.robocikd.joboffers.domain.offer.dto.JobOfferResponse;

import java.util.List;

public interface OfferFetchable {
    List<JobOfferResponse> fetchOffers();
}
