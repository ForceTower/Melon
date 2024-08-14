package com.forcetower.uefs.feature.unesaccount.why

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.databinding.FragmentServiceAccountWhyIsItNeededBinding
import com.forcetower.uefs.feature.shared.UFragment
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import java.nio.charset.Charset

class CreateAccountReasonsFragment : UFragment() {
    private lateinit var binding: FragmentServiceAccountWhyIsItNeededBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val markwon = Markwon.builder(requireContext())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    val color = ViewUtils.attributeColorUtils(requireContext(), androidx.appcompat.R.attr.colorPrimary)
                    builder.linkColor(color)
                }
            })
            .usePlugin(ImagesPlugin.create())
            .usePlugin(MovementMethodPlugin.create(LinkMovementMethod.getInstance()))
            .usePlugin(LinkifyPlugin.create())
            .build()

        val stream = requireContext().assets.open("unes_account_why.md")
        val size = stream.available()
        val buffer = ByteArray(size)
        stream.read(buffer)
        stream.close()
        val span = String(buffer, Charset.forName("UTF-8"))

        markwon.setMarkdown(binding.content, span)

        return FragmentServiceAccountWhyIsItNeededBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

}