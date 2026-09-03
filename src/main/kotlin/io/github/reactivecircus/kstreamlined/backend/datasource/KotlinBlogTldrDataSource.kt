package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContentPersister

interface KotlinBlogTldrDataSource

class RealKotlinBlogTldrDataSource(
    private val kotlinBlogContentPersister: KotlinBlogContentPersister,
) : KotlinBlogTldrDataSource
