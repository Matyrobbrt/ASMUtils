package com.matyrobbrt.asmutilsgradlehelper

import com.google.api.client.http.GenericUrl
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.w3c.dom.Element

import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

final class ASMUtilsGradleHelper {
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	static String resolveCommitMessage(String commitRef, String ghToken) throws IOException {
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

	static String resolveVersionFromCommitMessage(String cmtMessage) {
		var commitMessage = cmtMessage.toLowerCase(Locale.ROOT)
		final var startIndex = commitMessage.indexOf("[v")
		if (startIndex != -1) {
			commitMessage = commitMessage.substring(startIndex + 2)
			final var endIndex = commitMessage.indexOf(']');
			if (endIndex != -1) {
				var version = commitMessage.substring(0, endIndex)
				switch (version.toLowerCase(Locale.ROOT)) {
					case "major": version = getNextVersionFromMavenInfo(IncreaseType.MAJOR)
						break
					case "minor": version = getNextVersionFromMavenInfo(IncreaseType.MINOR)
						break
					case "bug": version = getNextVersionFromMavenInfo(IncreaseType.BUG)
				}
				return version;
			}
		}
		return getNextVersionFromMavenInfo(IncreaseType.MINOR)
	}

	static String getNextVersionFromMavenInfo(final IncreaseType increaseType) {
		final var parser = DocumentBuilderFactory.newInstance();
		parser.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

		final var builder = parser.newDocumentBuilder();
		try (final var is = new URL("https://repo1.maven.org/maven2/io/github/matyrobbrt/asmutils/maven-metadata.xml").openStream()) {
			final var xml = builder.parse(is);
			xml.getDocumentElement().normalize()
			var latestVersion = (((xml.getElementsByTagName("metadata").item(0) as Element)
					.getElementsByTagName("versioning").item(0) as Element)
					.getElementsByTagName("latest").item(0) as Element).getTextContent();
			var major = Integer.parseInt(latestVersion.substring(0, latestVersion.indexOf(".")))
			latestVersion = latestVersion.substring(latestVersion.indexOf(".") + 1)
			var minor = Integer.parseInt(latestVersion.substring(0, latestVersion.indexOf(".")))
			latestVersion = latestVersion.substring(latestVersion.indexOf("."))
			var bug = Integer.parseInt(latestVersion.substring(1))
			switch (increaseType) {
				case IncreaseType.MAJOR: {
					major++
					minor = 0
					bug = 0
				}
					break
				case IncreaseType.MINOR: {
					minor++
					bug = 0
				}
					break
				case IncreaseType.BUG: bug++
					break
			}
			return "$major.$minor.$bug"
		}
	}

	enum IncreaseType {
		MAJOR, MINOR, BUG;
	}
}