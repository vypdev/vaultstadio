/**
 * Federated activity type.
 */

package com.vaultstadio.app.domain.federation.model

enum class FederatedActivityType {
    SHARE_CREATED,
    SHARE_ACCEPTED,
    SHARE_DECLINED,
    FILE_ACCESSED,
    FILE_MODIFIED,
    COMMENT_ADDED,
    INSTANCE_ONLINE,
    INSTANCE_OFFLINE,
}
