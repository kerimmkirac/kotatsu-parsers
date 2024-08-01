package org.koitharu.kotatsu.parsers.site.mangareader.en

import org.koitharu.kotatsu.parsers.MangaLoaderContext
import org.koitharu.kotatsu.parsers.MangaSourceParser
import org.koitharu.kotatsu.parsers.model.MangaParserSource
import org.koitharu.kotatsu.parsers.site.mangareader.MangaReaderParser

@MangaSourceParser("ASCALONSCANS", "AscalonScans", "en")
internal class AscalonScans(context: MangaLoaderContext) :
	MangaReaderParser(context, MangaParserSource.ASCALONSCANS, "ascalonscans.com", pageSize = 20, searchPageSize = 10) {
	override val isTagsExclusionSupported = false
}
