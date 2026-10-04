package surge.domain.extension.interactor

import surge.domain.extension.repository.ExtensionStoreRepository

class GetExtensionStoreCountAsFlow(
    private val repository: ExtensionStoreRepository,
) {
    operator fun invoke() = repository.getCountAsFlow()
}
