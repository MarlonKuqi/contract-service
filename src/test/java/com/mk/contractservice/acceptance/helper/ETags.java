package com.mk.contractservice.acceptance.helper;

import com.mk.contractservice.controllers.shared.EntityTags;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

import static io.restassured.RestAssured.given;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ETags {

    public static String current(final String path, final Object... pathParams) {
        return given()
                .get(path, pathParams)
                .then()
                .statusCode(200)
                .extract().header(HttpHeaders.ETAG);
    }

    public static String ofVersion(final long version) {
        return EntityTags.of(version);
    }
}
