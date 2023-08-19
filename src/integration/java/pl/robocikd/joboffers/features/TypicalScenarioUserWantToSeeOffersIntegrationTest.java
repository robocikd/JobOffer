package pl.robocikd.joboffers.features;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import pl.robocikd.joboffers.BaseIntegrationTest;
import pl.robocikd.joboffers.SampleJobOfferResponse;
import pl.robocikd.joboffers.domain.offer.dto.OfferResponseDto;
import pl.robocikd.joboffers.infrastructure.offer.scheduler.HttpOffersScheduler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TypicalScenarioUserWantToSeeOffersIntegrationTest extends BaseIntegrationTest implements SampleJobOfferResponse {

    @Autowired
    HttpOffersScheduler httpOffersScheduler;

    @Container
    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"));

    @DynamicPropertySource
    public static void propertyOverride(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("offer.http.client.config.uri", () -> WIRE_MOCK_HOST);
        registry.add("offer.http.client.config.port", () -> wireMockServer.getPort());
    }

    @Test
    public void user_want_to_see_offers_but_have_to_be_logged_in_and_external_server_should_have_some_offers() throws Exception {
        // step 1: there are no offers in external HTTP server
        //given && when && then
        wireMockServer.stubFor(WireMock.get("/offers")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(bodyWithZeroOffersJson())));


        // step 2: scheduler ran 1st time and made GET to external server and system added 0 offers to database
        // given && when
        List<OfferResponseDto> newOffers = httpOffersScheduler.fetchAllOffersAndSaveAllIfNotExists();
        // then
//        assertThat(newOffers).isEmpty();


        //step 3: user tried to get JWT token by requesting POST /token with username=someUser, password=somePassword and system returned UNAUTHORIZED(401)
        //step 4: user made GET /offers with no jwt token and system returned UNAUTHORIZED(401)
        //step 5: user made POST /register with username=someUser, password=somePassword and system registered user with status OK(200)
        //step 6: user tried to get JWT token by requesting POST /token with username=someUser, password=somePassword and system returned OK(200) and jwttoken=AAAA.BBBB.CCC


        //step 7: user made GET /offers with header “Authorization: Bearer AAAA.BBBB.CCC” and system returned OK(200) with 0 offers
        // given
        String offerUrl = "/offers";
        // when
        ResultActions perform = mockMvc.perform(get(offerUrl)
                .contentType(MediaType.APPLICATION_JSON_VALUE));
        // then
        MvcResult mvcResult = perform.andExpect(status().isOk()).andReturn();
        String jsonWithOffers = mvcResult.getResponse().getContentAsString();
        List<OfferResponseDto> offerResponseDto = objectMapper.readValue(jsonWithOffers, List.class);
        assertThat(offerResponseDto).isEmpty();


        //step 8: there are 2 new offers in external HTTP server
        // given && when && then
        wireMockServer.stubFor(WireMock.get("/offers")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(bodyWithTwoOffersJson())));


        //step 9: scheduler ran 2nd time and made GET to external server and system added 2 new offers with ids: 1000 and 2000 to database
        // given && when
        List<OfferResponseDto> twoNewOffers = httpOffersScheduler.fetchAllOffersAndSaveAllIfNotExists();
        // then
        assertThat(twoNewOffers).hasSize(2);

        //step 10: user made GET /offers with header “Authorization: Bearer AAAA.BBBB.CCC” and system returned OK(200) with 2 offers with ids: 1000 and 2000
        // given && when
        ResultActions performGetForTwoOffers = mockMvc.perform(get(offerUrl)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        );
        // then
        MvcResult performGetForTwoOffersMvcResult = performGetForTwoOffers.andExpect(status().isOk()).andReturn();
        String jsonWithTwoOffers = performGetForTwoOffersMvcResult.getResponse().getContentAsString();
        List<OfferResponseDto> twoOffers = objectMapper.readValue(jsonWithTwoOffers, new TypeReference<>() {
        });
        assertThat(twoOffers).hasSize(2);
        OfferResponseDto expectedFirstOffer = twoNewOffers.get(0);
        OfferResponseDto expectedSecondOffer = twoNewOffers.get(1);
        assertThat(twoOffers).containsExactlyInAnyOrder(
                new OfferResponseDto(expectedFirstOffer.id(), expectedFirstOffer.companyName(), expectedFirstOffer.position(), expectedFirstOffer.salary(), expectedFirstOffer.offerUrl()),
                new OfferResponseDto(expectedSecondOffer.id(), expectedSecondOffer.companyName(), expectedSecondOffer.position(), expectedSecondOffer.salary(), expectedSecondOffer.offerUrl())
        );


        //step 11: user made GET /offers/9999 and system returned NOT_FOUND(404) with message “Offer with id 9999 not found”
        // given
        // when
        ResultActions performGetOffersNotExistingId = mockMvc.perform(get("/offers/9999"));
        // then
        performGetOffersNotExistingId.andExpect(status().isNotFound())
                .andExpect(content().json("""
                        {
                        "message":  "Offer with id 9999 not found",
                        "status": "NOT_FOUND"
                        }
                        """.trim()));

        //step 12: user made GET /offers/1000 and system returned OK(200) with offer
        // given
        String offerIdAddedToDatabase = expectedFirstOffer.id();
        // when
        ResultActions getOfferById = mockMvc.perform(get("/offers/" + offerIdAddedToDatabase)
                .contentType(MediaType.APPLICATION_JSON)
        );
        // then
        String singleOfferByOfferUrlJson = getOfferById.andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        OfferResponseDto singleOfferByOfferUrl = objectMapper.readValue(singleOfferByOfferUrlJson, OfferResponseDto.class);
        assertThat(singleOfferByOfferUrl).isEqualTo(expectedFirstOffer);


        //step 13: there are 2 new offers in external HTTP server
        // given && when && then
        wireMockServer.stubFor(WireMock.get("/offers")
                .willReturn(WireMock.aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader("Content-Type", "application/json")
                        .withBody(bodyWithFourOffersJson())));


        //step 14: scheduler ran 3rd time and made GET to external server and system added 2 new offers with ids: 3000 and 4000 to database
        // given && when
        List<OfferResponseDto> nextTwoNewOffers = httpOffersScheduler.fetchAllOffersAndSaveAllIfNotExists();
        // then
        assertThat(nextTwoNewOffers).hasSize(2);


        //step 15: user made GET /offers with header “Authorization: Bearer AAAA.BBBB.CCC” and system returned OK(200) with 4 offers with ids: 1000,2000, 3000 and 4000
        // given && when
        ResultActions performGetForFourOffers = mockMvc.perform(get(offerUrl)
                .contentType(MediaType.APPLICATION_JSON)
        );
        // then
        MvcResult performGetForFourOffersMvcResult = performGetForFourOffers.andExpect(status().isOk()).andReturn();
        String jsonWithFourOffers = performGetForFourOffersMvcResult.getResponse().getContentAsString();
        List<OfferResponseDto> fourOffers = objectMapper.readValue(jsonWithFourOffers, new TypeReference<>() {
        });
        assertThat(fourOffers).hasSize(4);
        OfferResponseDto expectedThirdOffer = nextTwoNewOffers.get(0);
        OfferResponseDto expectedFourthOffer = nextTwoNewOffers.get(1);
        assertThat(fourOffers).contains(
                new OfferResponseDto(expectedThirdOffer.id(), expectedThirdOffer.companyName(), expectedThirdOffer.position(), expectedThirdOffer.salary(), expectedThirdOffer.offerUrl()),
                new OfferResponseDto(expectedFourthOffer.id(), expectedFourthOffer.companyName(), expectedFourthOffer.position(), expectedFourthOffer.salary(), expectedFourthOffer.offerUrl()
                ));



        //step 16: user made POST /offers with header “Authorization: Bearer AAAA.BBBB.CCC” and offer as body and system returned CREATED(201) with saved offer
        // given
        // when
        ResultActions performPostOffersWithOneOffer = mockMvc.perform(post("/offers")
                .content("""
                        {
                        "companyName": "someCompany",
                        "position": "somePosition",
                        "salary": "7 000 - 9 000 PLN",
                        "offerUrl": "https://newoffers.pl/offer/1234"
                        }
                        """)
                .contentType(MediaType.APPLICATION_JSON)
        );
        // then
        String createdOfferJson = performPostOffersWithOneOffer.andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        OfferResponseDto parsedCreatedOfferJson = objectMapper.readValue(createdOfferJson, OfferResponseDto.class);
        String id = parsedCreatedOfferJson.id();
        assertAll(
                () -> assertThat(parsedCreatedOfferJson.offerUrl()).isEqualTo("https://newoffers.pl/offer/1234"),
                () -> assertThat(parsedCreatedOfferJson.companyName()).isEqualTo("someCompany"),
                () -> assertThat(parsedCreatedOfferJson.salary()).isEqualTo("7 000 - 9 000 PLN"),
                () -> assertThat(parsedCreatedOfferJson.position()).isEqualTo("somePosition"),
                () -> assertThat(id).isNotNull()
        );


        //step 17: user made GET /offers with header “Authorization: Bearer AAAA.BBBB.CCC” and system returned OK(200) with 5 offer
        // given & when
        ResultActions peformGetOffers = mockMvc.perform(get("/offers")
                .contentType(MediaType.APPLICATION_JSON)
        );
        // then
        String oneOfferJson = peformGetOffers.andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<OfferResponseDto> parsedJsonWithOneOffer = objectMapper.readValue(oneOfferJson, new TypeReference<>() {
        });
        assertThat(parsedJsonWithOneOffer).hasSize(5);
        assertThat(parsedJsonWithOneOffer.stream().map(OfferResponseDto::id)).contains(id);
    }
}

