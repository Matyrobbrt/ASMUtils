package com.matyrobbrt.asmutilsgradlehelper;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;

public final class ASMUtilsGradleHelper {
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	public static String resolveCommitMessage(String commitRef, String ghToken) throws IOException {
		final var rs = HTTP_TRANSPORT.createRequestFactory(request -> request
				.setRequestMethod("GET")
				.setHeaders(new HttpHeaders()
						.set("Content-Type", "application/json")
						.set("Authorization", "token " + ghToken)
				)).buildGetRequest(new GenericUrl("http://api.github.com/repos/Matyrobbrt/ASMUtils/commits/" + commitRef))
				.execute();

		final var response = new Gson().fromJson(rs.parseAsString(), JsonObject.class);
		return response.getAsJsonObject("commit").get("message").getAsString();
	}

}