/*
 * Copyright (C) 2019. by onlymash <im@fiepi.me>, All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package onlymash.flexbooru.entity.comment

import com.google.gson.annotations.SerializedName

data class CommentDanOne(
    @SerializedName("id")
    val id: Int,
    @SerializedName("score")
    val score: Int,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("post_id")
    val post_id: Int,
    @SerializedName("creator")
    val creator: String,
    @SerializedName("creator_id")
    val creator_id: Int,
    @SerializedName("body")
    val body: String
) : CommentBase() {
    override fun getPostId(): Int = post_id
    override fun getCommentId(): Int = id
    override fun getCommentBody(): String = body
    override fun getCommentDate(): CharSequence = createdAt
    override fun getCreatorId(): Int = creator_id
    override fun getCreatorName(): String = creator
}