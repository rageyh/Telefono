package org.mineacademy.fo.remain.nbt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PathUtil {

	private static final Pattern pattern = Pattern.compile("[^\\\\](\\.)");
	private static final Pattern indexPattern = Pattern.compile(".*\\[(-?[0-9]+)\\]");

	public static List<PathSegment> splitPath(String path) {
		final List<PathSegment> list = new ArrayList<>();
		final Matcher matcher = pattern.matcher(path);
		int startIndex = 0;
		while (matcher.find(startIndex)) {
			list.add(new PathSegment(path.substring(startIndex, matcher.end() - 1).replace("\\.", ".")));
			startIndex = matcher.end();
		}
		list.add(new PathSegment(path.substring(startIndex).replace("\\.", ".")));
		return list;
	}

	public static class PathSegment {

		private final String path;
		private final Integer index;

		private PathSegment(String path) {
			final Matcher matcher = indexPattern.matcher(path);
			if (matcher.find()) {
				this.path = path.substring(0, path.indexOf("["));
				this.index = Integer.parseInt(matcher.group(1));
			} else {
				this.path = path;
				this.index = null;
			}
		}

		public String getPath() {
			return this.path;
		}

		public int getIndex() {
			return this.index;
		}

		public boolean hasIndex() {
			return this.index != null;
		}

		@Override
		public String toString() {
			return "PathSegment [path=" + this.path + ", index=" + this.index + "]";
		}

	}

}
