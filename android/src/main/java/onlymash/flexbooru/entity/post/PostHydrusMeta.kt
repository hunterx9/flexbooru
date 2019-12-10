package onlymash.flexbooru.entity.post

data class PostHydrusMeta(
    val metadata: List<Metadata>
)

data class Metadata(
    val duration: Any,
    val file_id: Int,
    val has_audio: Boolean,
    val hash: String,
    val height: Int,
    val known_urls: List<String>,
    val mime: String,
    val num_frames: Any,
    val num_words: Any,
    val service_names_to_statuses_to_tags: ServiceNamesToStatusesToTags,
    val size: Int,
    val width: Int
)

data class ServiceNamesToStatusesToTags(
    val all_known_tags: AllKnownTags
)

class AllKnownTags(

    val my_tags: List<String>,
    val tag_repository: List<String>
)