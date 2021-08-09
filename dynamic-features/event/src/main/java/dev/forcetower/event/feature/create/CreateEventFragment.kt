/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.forcetower.event.feature.create

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import androidx.annotation.Keep
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.forcetower.core.extensions.isDarkTheme
import com.forcetower.core.utils.ColorUtils
import com.forcetower.core.utils.ViewUtils
import com.forcetower.uefs.GlideApp
import com.forcetower.uefs.core.injection.dependencies.EventModuleDependencies
import com.forcetower.uefs.core.model.unes.Course
import com.forcetower.uefs.core.model.unes.Event
import com.forcetower.uefs.feature.setup.CourseSelectionCallback
import com.forcetower.uefs.feature.setup.SelectCourseDialog
import com.forcetower.uefs.feature.shared.UFragment
import com.forcetower.uefs.feature.shared.getPixelsFromDp
import com.google.android.material.textfield.TextInputEditText
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import dagger.hilt.android.EntryPointAccessors
import dev.forcetower.event.R
import dev.forcetower.event.core.binding.formattedDate
import dev.forcetower.event.core.injection.DaggerEventComponent
import dev.forcetower.event.databinding.FragmentCreateEventBinding
import dev.forcetower.event.feature.details.EventDetailsActivity
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import javax.inject.Inject

@Keep
class CreateEventFragment : UFragment() {
    @Inject lateinit var factory: ViewModelProvider.Factory
    private val viewModel: CreationViewModel by viewModels { factory }
    private lateinit var binding: FragmentCreateEventBinding
    private val args by navArgs<CreateEventFragmentArgs>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerEventComponent.builder()
            .context(context)
            .dependencies(
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    EventModuleDependencies::class.java
                )
            )
            .build()
            .inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentCreateEventBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            if (args.eventId != 0L) {
                viewModel.loadModel(args.eventId).observe(
                    viewLifecycleOwner,
                    Observer {
                        it ?: return@Observer
                        populateInterface(it)
                    }
                )
            }
        }

        binding.image.setOnClickListener { pickImage() }
        binding.checkCertificate.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutCertificate.visibility = isChecked.toVisibility()
        }
        binding.checkFree.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutPrice.visibility = (!isChecked).toVisibility()
        }
        binding.checkOpenForAll.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutCourse.visibility = (!isChecked).toVisibility()
        }
        binding.checkRegisterPage.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutRegisterPage.visibility = isChecked.toVisibility()
        }

        binding.inputStartTime.setOnClickListener {
            binding.inputStartTime.error = null
            startDatePicker(binding.inputStartTime, true)
        }

        binding.inputEndTime.setOnClickListener {
            binding.inputEndTime.error = null
            startDatePicker(binding.inputEndTime, false)
        }

        binding.inputCourse.setOnClickListener {
            binding.inputCourse.error = null
            startCoursePicker()
        }

        binding.btnComplete.setOnClickListener { saveDataAndPreview() }
    }

    private fun startCoursePicker() {
        val dialog = SelectCourseDialog().apply {
            arguments = bundleOf("hide_description" to true)
        }
        dialog.setCallback(
            object : CourseSelectionCallback {
                override fun onSelected(course: Course) {
                    viewModel.selectedCourse = course
                    binding.inputCourse.setText(course.name)
                }
            }
        )
        dialog.show(childFragmentManager, "dialog_course")
    }

    private fun startDatePicker(input: TextInputEditText, start: Boolean) {
        val calendar = Calendar.getInstance()

        val selection = (if (start) viewModel.start else viewModel.end)
        calendar.timeInMillis = selection

        val color = ViewUtils.attributeColorUtils(requireContext(), com.forcetower.uefs.R.attr.colorPrimary)

        val picker = DatePickerDialog.newInstance(
            { _, y, m, d ->
                val next = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }.timeInMillis
                if (start) {
                    viewModel.start = next
                } else {
                    viewModel.end = next
                }
                showTimePicker(input, start)
            },
            calendar
        )
        picker.version = DatePickerDialog.Version.VERSION_2

        picker.accentColor = color
        picker.setOkColor(color)

        picker.isThemeDark = requireContext().isDarkTheme

        val colorOnSurfaceLight = ViewUtils.attributeColorUtils(requireContext(), com.forcetower.uefs.R.attr.colorOnSurfaceLight)
        picker.setCancelColor(colorOnSurfaceLight)

        picker.show(childFragmentManager, "date_picker_dialog")
    }

    private fun showTimePicker(input: TextInputEditText, start: Boolean) {
        val calendar = Calendar.getInstance()

        val selection = (if (start) viewModel.start else viewModel.end)
        calendar.timeInMillis = selection
        val picker = TimePickerDialog.newInstance(
            { _, h, m, s ->
                val next = Calendar.getInstance().apply {
                    timeInMillis = selection
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, s)
                }.timeInMillis
                if (start) {
                    viewModel.start = next
                } else {
                    viewModel.end = next
                }
                val zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(next), ZoneId.systemDefault())
                input.setText(zoned.toString())
            },
            true
        )

        picker.version = TimePickerDialog.Version.VERSION_2

        val color = ViewUtils.attributeColorUtils(requireContext(), com.forcetower.uefs.R.attr.colorPrimary)
        picker.accentColor = color
        picker.setOkColor(color)

        val colorOnSurfaceLight = ViewUtils.attributeColorUtils(requireContext(), com.forcetower.uefs.R.attr.colorOnSurfaceLight)
        picker.setCancelColor(colorOnSurfaceLight)

        picker.isThemeDark = requireContext().isDarkTheme

        picker.show(childFragmentManager, "time_picker_dialog")
    }

    private fun saveDataAndPreview() {
        val name = binding.inputName.text.toString()
        if (name.trim().length < 4) {
            binding.inputName.error = getString(R.string.event_error_too_small)
            return
        }

        val location = binding.inputLocation.text.toString()
        if (location.trim().length < 4) {
            binding.inputLocation.error = getString(R.string.event_error_too_small)
            return
        }

        val description = binding.inputDescription.text.toString()
        if (description.trim().length < 4) {
            binding.inputDescription.error = getString(R.string.event_error_too_small)
            return
        }

        val start = try {
            val text = binding.inputStartTime.text.toString()
            if (text.trim().length < 4) {
                showSnack(getString(R.string.event_date_invalid))
                return
            }
            val parsed = ZonedDateTime.parse(text)
            if (parsed.isBefore(ZonedDateTime.now())) {
                showSnack(getString(R.string.event_date_start_before_today))
                return
            }
            parsed
        } catch (error: Throwable) {
            showSnack(getString(R.string.how_did_you))
            return
        }

        val end = try {
            val text = binding.inputEndTime.text.toString()
            if (text.trim().length < 4) {
                showSnack(getString(R.string.event_date_invalid))
                return
            }
            val parsed = ZonedDateTime.parse(text)
            if (parsed.isBefore(start)) {
                showSnack(getString(R.string.event_date_end_before_start))
                return
            }
            parsed
        } catch (error: Throwable) {
            showSnack(getString(R.string.how_did_you))
            return
        }

        val offeredBy = binding.inputOfferedBy.text.toString()
        if (offeredBy.trim().length < 4) {
            binding.inputOfferedBy.error = getString(R.string.event_error_too_small)
            return
        }

        val free = binding.checkFree.isChecked
        val price = if (free) null else {
            try {
                binding.inputPrice.text.toString().toDouble()
            } catch (error: Throwable) {
                binding.inputPrice.error = getString(R.string.must_be_number)
                return
            }
        }
        val open = binding.checkOpenForAll.isChecked
        val courseId = if (!open) {
            val course = viewModel.selectedCourse
            if (course == null) {
                binding.inputCourse.error = getString(R.string.select_a_course)
                return
            } else {
                course.id.toInt()
            }
        } else {
            null
        }

        val register = binding.checkRegisterPage.isChecked
        val page = if (!register) {
            null
        } else {
            val text = binding.inputRegisterPage.text.toString()
            if (text.trim().length < 4) {
                binding.inputRegisterPage.error = getString(R.string.event_error_too_small)
                return
            }
            if (!URLUtil.isValidUrl(text)) {
                binding.inputRegisterPage.error = getString(R.string.event_not_valid_url)
                return
            }
            text
        }

        val certificate = binding.checkCertificate.isChecked
        val certificateHours = if (!certificate) {
            null
        } else {
            try {
                binding.inputCertificate.text.toString().toInt()
            } catch (error: Throwable) {
                error.printStackTrace()
                binding.inputCertificate.error = getString(R.string.certificate_hours_must_be_integer)
                return
            }
        }
        val image = viewModel.imageUri
        if (image == null) {
            showSnack(getString(R.string.must_select_image))
            return
        }

        viewModel.create(name, location, description, image, start, end, offeredBy, price, courseId, certificateHours, page)
            .observe(
                viewLifecycleOwner,
                Observer {
                    viewModel.createdId = it
                    preview(it)
                }
            )
    }

    private fun preview(id: Long) {
        val intent = Intent(requireContext(), EventDetailsActivity::class.java).apply {
            putExtra("eventId", id)
        }
        startActivityForResult(intent, REQUEST_PREVIEW_EVENT)
    }

    private fun populateInterface(event: Event) {
        GlideApp.with(binding.image).load(event.imageUrl).into(binding.image)
        binding.inputName.setText(event.name)
        binding.inputLocation.setText(event.location)
        binding.inputDescription.setText(event.description)
        formattedDate(binding.inputStartTime, event.startDate)
        formattedDate(binding.inputEndTime, event.endDate)
        binding.checkFree.isChecked = event.price != null
        binding.checkOpenForAll.isChecked = event.courseId != null
        binding.checkCertificate.isChecked = event.certificateHours != null
        if (event.price != null) {
            binding.inputPrice.setText("${event.price}")
        }
        if (event.courseId != null) {
            binding.inputCourse.setText("${event.courseId}")
        }
        if (event.certificateHours != null) {
            binding.inputCertificate.setText("${event.certificateHours}")
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        try {
            startActivityForResult(intent, REQUEST_SELECT_PICTURE)
        } catch (t: Throwable) {
            showSnack(getString(R.string.cant_start_activity_for_picking_image))
        }
    }

    private fun onImagePicked(uri: Uri) {
        GlideApp.with(binding.image).load(uri).into(binding.image)
        viewModel.imageUri = uri
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_SELECT_PICTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                    val uri = data.data!!

                    val bg = ColorUtils.modifyAlpha(ViewUtils.attributeColorUtils(requireContext(), com.forcetower.uefs.R.attr.colorPrimary), 120)
                    val ac = ViewUtils.attributeColorUtils(requireContext(), com.forcetower.uefs.R.attr.colorAccent)
                    CropImage.activity(uri)
                        .setFixAspectRatio(true)
                        .setAspectRatio(4, 3)
                        .setCropShape(CropImageView.CropShape.RECTANGLE)
                        .setBackgroundColor(bg)
                        .setBorderLineColor(ac)
                        .setBorderCornerColor(ac)
                        .setActivityMenuIconColor(ac)
                        .setBorderLineThickness(getPixelsFromDp(requireContext(), 2))
                        .setActivityTitle(getString(R.string.cut_event_image))
                        .setGuidelines(CropImageView.Guidelines.OFF)
                        .start(requireContext(), this)
                }
            }
            REQUEST_PREVIEW_EVENT -> {
                Timber.d("Result code $resultCode")
                if (resultCode == Activity.RESULT_OK) {
                    val created = viewModel.createdId
                    if (created == null) {
                        showSnack(getString(R.string.some_bad_stuff_happened))
                        return
                    }
                    viewModel.confirmCreate(created)
                    showSnack(getString(R.string.event_scheduled_to_send))
                    findNavController().popBackStack()
                }
            }
            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                val result = CropImage.getActivityResult(data)
                if (resultCode == Activity.RESULT_OK) {
                    val imageUri = result.uri
                    onImagePicked(imageUri)
                }
            }
        }
    }

    private fun Boolean.toVisibility(): Int {
        return if (this) View.VISIBLE else View.GONE
    }

    companion object {
        private const val REQUEST_SELECT_PICTURE = 9000
        private const val REQUEST_PREVIEW_EVENT = 10000
    }
}
