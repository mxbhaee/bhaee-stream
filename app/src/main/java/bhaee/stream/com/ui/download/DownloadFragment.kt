package bhaee.stream.com.ui.download

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import bhaee.stream.com.R
import bhaee.stream.com.isMovieType
import bhaee.stream.com.mvvm.observe
import bhaee.stream.com.ui.download.DownloadButtonSetup.handleDownloadClick
import bhaee.stream.com.utils.AppUtils.loadResult
import bhaee.stream.com.utils.Coroutines.main
import bhaee.stream.com.utils.DOWNLOAD_EPISODE_CACHE
import bhaee.stream.com.utils.DataStore
import bhaee.stream.com.utils.UIHelper.fixPaddingStatusbar
import bhaee.stream.com.utils.UIHelper.hideKeyboard
import bhaee.stream.com.utils.VideoDownloadHelper
import bhaee.stream.com.utils.VideoDownloadManager
import kotlinx.android.synthetic.main.fragment_downloads.*

const val DOWNLOAD_NAVIGATE_TO = "downloadpage"

class DownloadFragment : Fragment() {
    private lateinit var downloadsViewModel: DownloadViewModel

    private fun getBytesAsText(bytes: Long): String {
        return "%.1f".format(bytes / 1000000000f)
    }

    private fun View.setLayoutWidth(weight: Long) {
        val param = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.MATCH_PARENT,
            maxOf((weight / 1000000000f), 0.1f) // 100mb
        )
        this.layoutParams = param
    }

    private fun setList(list: List<VisualDownloadHeaderCached>) {
        main {
            (download_list?.adapter as DownloadHeaderAdapter?)?.cardList = list
            download_list?.adapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        (download_list?.adapter as DownloadHeaderAdapter?)?.killAdapter()
        super.onDestroyView()
    }

    override fun onDestroy() {
        if(downloadDeleteEventListener != null) {
            VideoDownloadManager.downloadDeleteEvent -= downloadDeleteEventListener!!
            downloadDeleteEventListener = null
        }
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        downloadsViewModel =
            ViewModelProvider(this).get(DownloadViewModel::class.java)
        observe(downloadsViewModel.noDownloadsText) {
            text_no_downloads.text = it
        }
        observe(downloadsViewModel.headerCards) {
            setList(it)
            download_loading.isVisible = false
        }
        observe(downloadsViewModel.availableBytes) {
            download_free_txt?.text =
                getString(R.string.storage_size_format).format(getString(R.string.free_storage), getBytesAsText(it))
            download_free?.setLayoutWidth(it)
        }
        observe(downloadsViewModel.usedBytes) {
            download_used_txt?.text =
                getString(R.string.storage_size_format).format(getString(R.string.used_storage), getBytesAsText(it))
            download_used?.setLayoutWidth(it)
        }
        observe(downloadsViewModel.downloadBytes) {
            download_app_txt?.text =
                getString(R.string.storage_size_format).format(getString(R.string.app_storage), getBytesAsText(it))
            download_app?.setLayoutWidth(it)
            download_storage_appbar?.visibility = View.VISIBLE
        }
        return inflater.inflate(R.layout.fragment_downloads, container, false)
    }

    private var downloadDeleteEventListener: ((Int) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideKeyboard()

        val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder> =
            DownloadHeaderAdapter(
                ArrayList(),
                { click ->
                    when (click.action) {
                        0 -> {
                            if (click.data.type.isMovieType()) {
                                //wont be called
                            } else {
                                val folder = DataStore.getFolderName(DOWNLOAD_EPISODE_CACHE, click.data.id.toString())
                                val navHostFragment = activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment?
                                navHostFragment?.navController?.navigate(
                                    R.id.navigation_download_child,
                                    DownloadChildFragment.newInstance(click.data.name, folder)
                                )
                            }
                        }
                        1 -> {
                            (activity as AppCompatActivity?)?.loadResult(click.data.url, click.data.apiName)
                        }
                    }

                },
                { downloadClickEvent ->
                    if (downloadClickEvent.data !is VideoDownloadHelper.DownloadEpisodeCached) return@DownloadHeaderAdapter
                    handleDownloadClick(activity, downloadClickEvent.data.name, downloadClickEvent)
                    if (downloadClickEvent.action == DOWNLOAD_ACTION_DELETE_FILE) {
                        context?.let { ctx ->
                            downloadsViewModel.updateList(ctx)
                        }
                    }
                }
            )

        downloadDeleteEventListener = { id ->
            val list = (download_list?.adapter as DownloadHeaderAdapter?)?.cardList
            if (list != null) {
                if (list.any { it.data.id == id }) {
                    context?.let { ctx ->
                        setList(ArrayList())
                        downloadsViewModel.updateList(ctx)
                    }
                }
            }
        }

        downloadDeleteEventListener?.let { VideoDownloadManager.downloadDeleteEvent += it }

        download_list.adapter = adapter
        download_list.layoutManager = GridLayoutManager(context, 1)
        downloadsViewModel.updateList(requireContext())

        context?.fixPaddingStatusbar(download_root)
    }
}