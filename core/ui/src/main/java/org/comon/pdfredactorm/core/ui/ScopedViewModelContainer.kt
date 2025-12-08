package org.comon.pdfredactorm.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
fun ScopedViewModelContainer(
    content: @Composable () -> Unit
) {
    val parent = LocalViewModelStoreOwner.current
    val viewModelStoreOwner = remember(parent) {
        object : ViewModelStoreOwner, androidx.lifecycle.HasDefaultViewModelProviderFactory {
            override val viewModelStore = ViewModelStore()

            override val defaultViewModelProviderFactory: androidx.lifecycle.ViewModelProvider.Factory
                get() = (parent as? androidx.lifecycle.HasDefaultViewModelProviderFactory)
                    ?.defaultViewModelProviderFactory
                    ?: androidx.lifecycle.ViewModelProvider.NewInstanceFactory.instance

            override val defaultViewModelCreationExtras: androidx.lifecycle.viewmodel.CreationExtras
                get() = (parent as? androidx.lifecycle.HasDefaultViewModelProviderFactory)
                    ?.defaultViewModelCreationExtras
                    ?: androidx.lifecycle.viewmodel.CreationExtras.Empty
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModelStoreOwner.viewModelStore.clear()
        }
    }

    CompositionLocalProvider(
        LocalViewModelStoreOwner provides viewModelStoreOwner
    ) {
        content()
    }
}
