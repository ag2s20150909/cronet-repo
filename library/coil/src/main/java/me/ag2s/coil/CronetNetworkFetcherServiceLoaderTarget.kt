package me.ag2s.coil

import coil3.Uri
import coil3.annotation.ExperimentalCoilApi
import coil3.annotation.InternalCoilApi
import coil3.fetch.Fetcher
import coil3.network.NetworkFetcher
import coil3.util.FetcherServiceLoaderTarget
import me.ag2s.cronet.CronetHolder


@OptIn(InternalCoilApi::class)
class CronetNetworkFetcherServiceLoaderTarget: FetcherServiceLoaderTarget<Uri>  {
    @OptIn(ExperimentalCoilApi::class)
    override fun factory(): Fetcher.Factory<Uri>? {
       return NetworkFetcher.Factory(
            networkClient = {
                CronetNetworkClient(cronet = CronetHolder.getEngine())
            }
        )
    }

    override fun type()=Uri::class

    override fun priority(): Int {
        return 5
    }
}