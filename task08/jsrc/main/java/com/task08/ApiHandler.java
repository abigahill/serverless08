package com.task08;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.openmeteo.api.OpenMeteoApi;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.lambda.LambdaLayer;
import com.syndicate.deployment.annotations.lambda.LambdaUrlConfig;
import com.syndicate.deployment.model.Architecture;
import com.syndicate.deployment.model.ArtifactExtension;
import com.syndicate.deployment.model.DeploymentRuntime;
import com.syndicate.deployment.model.RetentionSetting;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;
import java.util.Map;

@LambdaHandler(lambdaName = "api_handler",
	roleName = "api_handler-role",
	isPublishVersion = false,
	layers = {"sdk-layer"},
	logsExpiration = RetentionSetting.SYNDICATE_ALIASES_SPECIFIED
)
@LambdaLayer(
		layerName = "sdk-layer",
		libraries = {"libs/open-meteo-api.jar"},
		runtime = DeploymentRuntime.JAVA11,
		architectures = {Architecture.ARM64},
		artifactExtension = ArtifactExtension.ZIP
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class ApiHandler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {
	private static final String OK_METHOD = "GET";
	private static final String OK_PATH = "/weather";
	private static final int SC_OK = 200;
	private static final int SC_NOT_FOUND = 404;
	private static final int SC_INTERNAL_SERVER_ERROR = 500;
	private final Map<String, String> responseHeaders = Map.of("Content-Type", "application/json");

	public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
		LambdaLogger logger = context.getLogger();
		String path = event.getRequestContext().getHttp().getPath();
		String method = event.getRequestContext().getHttp().getMethod();
		logger.log("Request Method: " + method + " and Request path: " + path);

		if (OK_METHOD.equals(method) && OK_PATH.equals(path)) {
			return getWeather();
		} else {
			return badRequestResponse(method, path);
		}
	}

	private APIGatewayV2HTTPResponse getWeather() {
		String weather = OpenMeteoApi.getWeatherForecast();
		if (weather != null) {
			return getResponse(SC_OK, weather);
		} else {
			return getResponse(SC_INTERNAL_SERVER_ERROR, "Weather not available!");
		}
	}

	private APIGatewayV2HTTPResponse badRequestResponse(String method, String path) {
		return getResponse(SC_NOT_FOUND, "No resource exists with method "
			+ method + " and path " + path);
	}

	private APIGatewayV2HTTPResponse getResponse(int statusCode, String body) {
		return APIGatewayV2HTTPResponse.builder()
		.withStatusCode(statusCode)
		.withHeaders(responseHeaders)
		.withBody(body)
		.build();
	}
}
