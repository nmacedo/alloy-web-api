package pt.haslab.alloywebapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

@QuarkusTest
class AlloyModelExamplesTest {

	@Test
	void validatesStaticModel() throws IOException {
		validate("static/valid.als", true);
	}

	@Test
	void validatesDynamicModel() throws IOException {
		validate("dynamic/valid.als", true);
	}

	@Test
	void rejectsSyntacticallyInvalidStaticModel() throws IOException {
		validate("static/invalid.als", false);
	}

	@Test
	void rejectsSyntacticallyInvalidDynamicModel() throws IOException {
		validate("dynamic/invalid.als", false);
	}

	@Test
	void enumeratesStaticModelInstances() throws IOException {
		enumerate("static/valid.als", "static");
	}

	@Test
	void enumeratesDynamicModelInstances() throws IOException {
		enumerate("dynamic/valid.als", "dynamic");
	}

	@Test
	void reportsStaticModelWarning() throws IOException {
		reportsWarning("static/warning.als");
	}

	@Test
	void reportsDynamicModelWarning() throws IOException {
		reportsWarning("dynamic/warning.als");
	}

	private void validate(String fixture, boolean expectedSuccess) throws IOException {
		String response = given()
				.contentType(ContentType.JSON)
				.body(jsonModel(fixture))
				.when().post("/validate")
				.then().statusCode(200)
					.body("success", equalTo(expectedSuccess))
					.extract().asString();

		if (!expectedSuccess) {
			JsonPath result = JsonPath.from(response);
			org.junit.jupiter.api.Assertions.assertNotNull(result.getString("errorMessage"));
			org.junit.jupiter.api.Assertions.assertNotNull(result.get("errorLocation.line"));
		}
	}

	private void enumerate(String fixture, String sessionPrefix) throws IOException {
		String response = given()
				.contentType(ContentType.JSON)
				.body(jsonInstancesRequest(fixture, sessionPrefix))
				.when().post("/getInstances")
				.then().statusCode(200)
					.body("[0].unsat", is(false))
					.body("[0].instance", notNullValue())
					.extract().asString();

		if ("static".equals(sessionPrefix)) {
			given().contentType(ContentType.JSON).body(response)
					.then().body("[0].static", is(true));
		} else {
			given().contentType(ContentType.JSON).body(response)
					.then().body("[0].static", is(false));
		}
	}

	private void reportsWarning(String fixture) throws IOException {
		given()
				.contentType(ContentType.JSON)
				.body(jsonInstancesRequest(fixture, "warning"))
				.when().post("/getInstances")
				.then().statusCode(200)
					.body("[0].warning_error", equalTo(true))
					.body("[0].msg", notNullValue());
	}

	private String jsonModel(String fixture) throws IOException {
		return "{\"model\":" + quote(readFixture(fixture)) + "}";
	}

	private String jsonInstancesRequest(String fixture, String prefix) throws IOException {
		return "{\"model\":" + quote(readFixture(fixture))
				+ ",\"numberOfInstances\":1,\"commandIndex\":0"
				+ ",\"sessionId\":" + quote(prefix + "-" + UUID.randomUUID())
				+ ",\"parentId\":\"\"}";
	}

	private String readFixture(String fixture) throws IOException {
		String resource = "/models/" + fixture;
		try (InputStream stream = getClass().getResourceAsStream(resource)) {
			if (stream == null) {
				throw new IllegalArgumentException("Missing test model fixture: " + resource);
			}
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private String quote(String value) {
		return "\"" + value.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r") + "\"";
	}
}
