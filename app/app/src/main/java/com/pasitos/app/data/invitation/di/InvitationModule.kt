package com.pasitos.app.data.invitation.di

import com.pasitos.app.data.invitation.InvitationApiRepository
import com.pasitos.app.data.invitation.InvitationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class InvitationModule {

    @Binds
    abstract fun bindInvitationRepository(impl: InvitationApiRepository): InvitationRepository
}
