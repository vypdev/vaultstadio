package com.vaultstadio.domain.share.repository

/** Owner management composes narrower read/lifecycle ports; public admission is a separate capability. */
interface ShareRepository : ShareLookupRepository, ShareLifecycleRepository
