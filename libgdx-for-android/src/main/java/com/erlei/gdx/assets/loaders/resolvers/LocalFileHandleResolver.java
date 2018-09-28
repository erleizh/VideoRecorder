
package com.erlei.gdx.assets.loaders.resolvers;

import com.erlei.gdx.Gdx;
import com.erlei.gdx.assets.loaders.FileHandleResolver;
import com.erlei.gdx.files.FileHandle;

public class LocalFileHandleResolver implements FileHandleResolver {
	@Override
	public FileHandle resolve (String fileName) {
		return Gdx.files.local(fileName);
	}
}
