package pt.haslab.alloywebapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
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
	void projectsAnInstanceFromAnExistingSolverSession() throws IOException {
		String sessionId = "projection-" + UUID.randomUUID();
		String model = "sig A { r: B -> C }\nsig B {}\nsig C {}\nrun { some A.r } for 2";

		String instances = given()
				.contentType(ContentType.JSON)
				.body(jsonInstancesRequestForModel(model, sessionId))
				.when().post("/getInstances")
				.then().statusCode(200)
					.body("[0].unsat", is(false))
					.extract().asString();
		Assertions.assertEquals(List.of("A", "B", "C"), relationTypes(instances,
				"[0].instance[0].rels", "r"));
		List<Map<String, Object>> instanceTypes = JsonPath.from(instances)
				.getList("[0].instance[0].types");
		String focusedAtom = instanceTypes.stream()
				.filter(type -> "A".equals(type.get("name")))
				.map(type -> (List<String>) type.get("atoms"))
				.filter(atoms -> !atoms.isEmpty())
				.findFirst()
				.orElseThrow(() -> new AssertionError("Fixture produced no A atoms"))
				.get(0);

		String projected = given()
				.contentType(ContentType.JSON)
				.body("{\"sessionId\":\"" + sessionId
						+ "\",\"type\":[\"" + focusedAtom + "\"],\"index\":0}")
				.when().post("/getProjection")
				.then().statusCode(200)
					.body("[0].types", notNullValue())
					.extract().asString();
		Assertions.assertFalse(relationTypes(projected, "[0].rels", "r").isEmpty());
	}

	@SuppressWarnings("unchecked")
	private List<String> relationTypes(String response, String relationsPath, String relationName) {
		List<Map<String, Object>> relations = JsonPath.from(response).getList(relationsPath);
		return relations.stream()
				.filter(relation -> relationName.equals(relation.get("name")))
				.map(relation -> (List<String>) relation.get("types"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Missing relation " + relationName));
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
		return jsonInstancesRequestForModel(readFixture(fixture), prefix + "-" + UUID.randomUUID());
	}

	private String jsonInstancesRequestForModel(String model, String sessionId) {
		return "{\"model\":" + quote(model)
				+ ",\"numberOfInstances\":1,\"commandIndex\":0"
				+ ",\"sessionId\":" + quote(sessionId)
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
