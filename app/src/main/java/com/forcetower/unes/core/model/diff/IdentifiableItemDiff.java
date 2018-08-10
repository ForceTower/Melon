package com.forcetower.unes.core.model.diff;

import com.forcetower.sagres.database.Identifiable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class IdentifiableItemDiff<T extends Identifiable> extends DiffUtil.ItemCallback<T> {
    @Override
    public boolean areItemsTheSame(@NonNull T oldItem, @NonNull T newItem) {
        return oldItem.getUid() == newItem.getUid();
    }

    @Override
    public boolean areContentsTheSame(@NonNull T oldItem, @NonNull T newItem) {
        return oldItem.equals(newItem);
    }
}
