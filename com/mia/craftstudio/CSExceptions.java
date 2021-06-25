package com.mia.craftstudio;

public class CSExceptions extends RuntimeException {
	public CSExceptions() {}
	public CSExceptions(final String message) { super("CraftStudio Library Exception: " + message); }
	public CSExceptions(final Throwable cause) { super(cause); }
	public CSExceptions(final String message, final Throwable cause) { super("CraftStudio Library Exception: " + message, cause); }
	public CSExceptions(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super("CraftStudio Library Exception: " + message, cause, enableSuppression, writableStackTrace);
	}

	public static class NoCSProjectException extends CSExceptions {
		public NoCSProjectException() {
			super("Programming error: CSPacks must be added to a CSProject to be used.");
		}
	}
	public static class TypeMismatchException extends CSExceptions {
		public TypeMismatchException(final Object expected, final Object received) {
			super(String.format("The file type being loaded(%s) does not match the expcted type(%s).", received.toString(), expected.toString()));
		}
	}
	public static class UnsupportedVersionException extends CSExceptions {
		public UnsupportedVersionException(final Object version) {
			super(String.format("This version of CraftStudioLib is unable to load the passed in file version(%s)", version.toString()));
		}
	}
	public static class ResourcesNotFoundException extends CSExceptions {
		public ResourcesNotFoundException(final Object project) {
			super(String.format("CraftStudioLib is unable to locate any resources for CSProject: %s", project.toString()));
		}
	}
	public static class DuplicateProjectException extends CSExceptions {
		public DuplicateProjectException(final Object project) {
			super(String.format("CraftStudioLib is unable to add CSProject: %s; There is a duplicate project with the same ID", project.toString()));
		}
	}
}
