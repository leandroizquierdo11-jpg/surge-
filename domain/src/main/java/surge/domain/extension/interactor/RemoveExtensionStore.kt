package surge.domain.extension.interactor

import surge.domain.extension.repository.ExtensionStoreRepository

class RemoveExtensionStore(
    private val repository: ExtensionStoreRepository,
) {
    suspend operator fun invoke(indexUrl: String) {
        repository.remove(indexUrl)
    }
}
