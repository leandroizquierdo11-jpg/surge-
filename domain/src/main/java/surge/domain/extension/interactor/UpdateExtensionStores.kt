package surge.domain.extension.interactor

import surge.domain.extension.repository.ExtensionStoreRepository

class UpdateExtensionStores(
    private val repository: ExtensionStoreRepository,
) {
    suspend operator fun invoke() {
        repository.refreshAll()
    }
}
