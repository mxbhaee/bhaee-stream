package bhaee.stream.com.syncproviders.providers

import bhaee.stream.com.syncproviders.OAuth2API

//TODO dropbox sync
class Dropbox : OAuth2API {
    override val idPrefix = "dropbox"
    override var name = "Dropbox"
    override val key = "zlqsamadlwydvb2"
    override val redirectUrl = "dropboxlogin"

    override fun authenticate() {
        TODO("Not yet implemented")
    }

    override suspend fun handleRedirect(url: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun logOut() {
        TODO("Not yet implemented")
    }

    override fun loginInfo(): OAuth2API.LoginInfo? {
        TODO("Not yet implemented")
    }
}