/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.utils

import android.net.Uri

object SearchRoutes {
    const val ROUTE = "search/{query}"

    // Prefix query payloads so a literal "null" search is not confused with
    // an absent or undefined Navigation argument.
    private const val QUERY_ROUTE_PREFIX = "__Mixora_search_query__"

    // Encode the prefixed query as a route path segment before navigating.
    fun resultRoute(query: String): String =
        "search/${Uri.encode(QUERY_ROUTE_PREFIX + query)}"

    // Navigation already decodes path segments once, so only strip the prefix.
    // Decoding again would turn literal input like "%2F" into "/".
    fun decodeQuery(rawQuery: String): String =
        rawQuery.removePrefix(QUERY_ROUTE_PREFIX)
}
