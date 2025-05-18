package moe.emi.finite.components.list.ui.adapter.java;

import androidx.annotation.NonNull;

import com.xwray.groupie.Group;
import com.xwray.groupie.Item;

import java.util.Collection;

class GroupUtils {
	@NonNull
	static Item getItem(Collection<? extends Group> groups, int position) {
		int previousPosition = 0;

		for (Group group : groups) {
			int size = group.getItemCount();
			if (size + previousPosition > position) {
				return group.getItem(position - previousPosition);
			}
			previousPosition += size;
		}

		throw new IndexOutOfBoundsException("Wanted item at " + position + " but there are only "
				+ previousPosition + " items");
	}

	static int getItemCount(@NonNull Collection<? extends Group> groups) {
		int size = 0;
		for (Group group : groups) {
			size += group.getItemCount();
		}
		return size;
	}
}