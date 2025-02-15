package org.koitharu.kotatsu.parsers.site.wpcomics.vi

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.config.ConfigKey
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN
import org.koitharu.kotatsu.parsers.site.wpcomics.WpComicsParser
import org.koitharu.kotatsu.parsers.util.*
import java.text.SimpleDateFormat

@MangaSourceParser("NETTRUYEN", "NetTruyen", "vi")
internal class NetTruyen(context: MangaLoaderContext) :
	WpComicsParser(context, MangaParserSource.NETTRUYEN, "nettruyenrr.com", 36) {

	override val configKeyDomain: ConfigKey.Domain = ConfigKey.Domain(
		"nettruyenrr.com",
		"nettruyenx.com"
	)

	override suspend fun getDetails(manga: Manga): Manga = coroutineScope {
		val fullUrl = manga.url.toAbsoluteUrl(domain)
		val docDeferred = async { webClient.httpGet(fullUrl).parseHtml() }
		val chaptersDeferred = async { fetchChapters(manga.url) }
		val tagMap = getOrCreateTagMap()
		val doc = docDeferred.await()
		val tagsElement = doc.select("li.kind p.col-xs-8 a")
		val mangaTags = tagsElement.mapNotNullToSet { tagMap[it.text()] }
		manga.copy(
			description = doc.selectFirst(selectDesc)?.html(),
			altTitle = doc.selectFirst("h2.other-name")?.textOrNull(),
			author = doc.body().select(selectAut).textOrNull(),
			state = doc.selectFirst(selectState)?.let {
				when (it.text()) {
					in ongoing -> MangaState.ONGOING
					in finished -> MangaState.FINISHED
					else -> null
				}
			},
			tags = mangaTags,
			rating = doc.selectFirst("div.star input")?.attr("value")?.toFloatOrNull()?.div(5f) ?: RATING_UNKNOWN,
			chapters = chaptersDeferred.await(),
		)
	}

	private suspend fun fetchChapters(mangaUrl: String): List<MangaChapter> {
		val slug = mangaUrl.substringAfterLast('/').substringBeforeLast('-')
		val id = mangaUrl.substringAfterLast('-')
		val chaptersUrl = "/Comic/Services/ComicService.asmx/ChapterList?slug=$slug&comicId=$id".toAbsoluteUrl(domain)
		val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

		val data = webClient.httpGet(chaptersUrl).parseJson().getJSONArray("data")
		return List(data.length()) { i ->
			val jo = data.getJSONObject(data.length() - 1 - i)
			val chapterSlug = jo.getString("chapter_slug")
			val chapterUrl = "/truyen-tranh/$slug/$chapterSlug"

			MangaChapter(
				id = generateUid(chapterUrl),
				name = jo.getString("chapter_name"),
				number = i + 1f,
				volume = 0,
				url = chapterUrl,
				scanlator = null,
				uploadDate = df.tryParse(jo.getString("updated_at")),
				branch = null,
				source = source,
			)
		}
	}
}
