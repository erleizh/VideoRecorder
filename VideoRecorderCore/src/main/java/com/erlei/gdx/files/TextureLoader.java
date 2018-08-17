/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.erlei.gdx.files;

import com.erlei.gdx.files.assets.AssetDescriptor;
import com.erlei.gdx.files.assets.AssetLoader;
import com.erlei.gdx.files.assets.AssetLoaderParameters;
import com.erlei.gdx.files.assets.AssetManager;
import com.erlei.gdx.files.assets.AsynchronousAssetLoader;
import com.erlei.gdx.files.assets.FileHandleResolver;
import com.erlei.gdx.graphics.Texture;
import com.erlei.gdx.graphics.Texture.TextureWrap;
import com.erlei.gdx.graphics.TextureData;
import com.erlei.gdx.utils.Pixmap;

import java.util.ArrayList;
import java.util.List;

/** {@link AssetLoader} for {@link Texture} instances. The pixel data is loaded asynchronously. The texture is then created on the
 * rendering thread, synchronously. Passing a {@link TextureParameter} to
 * {@link AssetManager#load(String, Class, AssetLoaderParameters)} allows one to specify parameters as can be passed to the
 * various Texture constructors, e.g. filtering, whether to generate mipmaps and so on.
 * @author mzechner */
public class TextureLoader extends AsynchronousAssetLoader<Texture, TextureLoader.TextureParameter> {
	static public class TextureLoaderInfo {
		String filename;
		TextureData data;
		Texture texture;
	};

	TextureLoaderInfo info = new TextureLoaderInfo();

	public TextureLoader(FileHandleResolver resolver) {
		super(resolver);
	}

	@Override
	public void loadAsync (AssetManager manager, String fileName, FileHandle file, TextureParameter parameter) {
		info.filename = fileName;
		if (parameter == null || parameter.textureData == null) {
			Pixmap pixmap = null;
			Pixmap.Format format = null;
			boolean genMipMaps = false;
			info.texture = null;

			if (parameter != null) {
				format = parameter.format;
				genMipMaps = parameter.genMipMaps;
				info.texture = parameter.texture;
			}

			info.data = TextureData.Factory.loadFromFile(file, format, genMipMaps);
		} else {
			info.data = parameter.textureData;
			info.texture = parameter.texture;
		}
		if (!info.data.isPrepared()) info.data.prepare();
	}

	@Override
	public Texture loadSync (AssetManager manager, String fileName, FileHandle file, TextureParameter parameter) {
		if (info == null) return null;
		Texture texture = info.texture;
		if (texture != null) {
			texture.load(info.data);
		} else {
			texture = new Texture(info.data);
		}
		if (parameter != null) {
			texture.setFilter(parameter.minFilter, parameter.magFilter);
			texture.setWrap(parameter.wrapU, parameter.wrapV);
		}
		return texture;
	}

	@Override
	public ArrayList<AssetDescriptor> getDependencies (String fileName, FileHandle file, TextureParameter parameter) {
		return null;
	}

	static public class TextureParameter extends AssetLoaderParameters<Texture> {
		/** the format of the final Texture. Uses the source images format if null **/
		public Pixmap.Format format = null;
		/** whether to generate mipmaps **/
		public boolean genMipMaps = false;
		/** The texture to put the {@link TextureData} in, optional. **/
		public Texture texture = null;
		/** TextureData for textures created on the fly, optional. When set, all format and genMipMaps are ignored */
		public TextureData textureData = null;
		public Texture.TextureFilter minFilter = Texture.TextureFilter.Nearest;
		public Texture.TextureFilter magFilter = Texture.TextureFilter.Nearest;
		public TextureWrap wrapU = TextureWrap.ClampToEdge;
		public TextureWrap wrapV = TextureWrap.ClampToEdge;
	}
}
