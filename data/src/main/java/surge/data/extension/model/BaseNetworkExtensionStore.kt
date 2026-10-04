package surge.data.extension.model

import surge.domain.extension.model.ExtensionStore

interface BaseNetworkExtensionStore {
    fun toExtensionStore(indexUrl: String): ExtensionStore
}
