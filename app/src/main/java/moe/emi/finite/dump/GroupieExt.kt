package moe.emi.finite.dump

import com.xwray.groupie.Group
import com.xwray.groupie.Section

inline fun <reified T : Group> Section.forEvery(action: (T) -> Unit) {
	for (i in this.groups) if (i is T) action(i)
}