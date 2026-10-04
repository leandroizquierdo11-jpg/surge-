package surge.domain.extension.interactor

import surge.domain.extension.repository.ExtensionStoreRepository

class AddExtensionStore(
    private val repository: ExtensionStoreRepository,
) {
    suspend operator fun invoke(indexUrl: String): Result<Unit> {
        return repository.insert(indexUrl)
    }
}
