#if 0
__FBSDID("$FreeBSD: src/usr.bin/bsdiff/bspatch/bspatch.c,v 1.1 2005/08/06 01:59:06 cperciva Exp $");
#endif

// 很多文件没引入进来
#include "bzlib.c"
#include "crctable.c"
#include "compress.c"
#include "decompress.c"
#include "randtable.c"
#include "blocksort.c"
#include "huffman.c"
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <err.h>
#include <unistd.h>
#include <fcntl.h>
#include "com_hc_essay_library_util_PatchUtils.h"

static off_t offtin(u_char *buf)
{
	off_t y;

	y=buf[7]&0x7F;
	y=y*256;y+=buf[6];
	y=y*256;y+=buf[5];
	y=y*256;y+=buf[4];
	y=y*256;y+=buf[3];
	y=y*256;y+=buf[2];
	y=y*256;y+=buf[1];
	y=y*256;y+=buf[0];

	if(buf[7]&0x80) y=-y;

	return y;
}
// 第三方的合并方法
int combine(int argc,char * argv[])
{
	FILE * f, * cpf, * dpf, * epf;
	BZFILE * cpfbz2, * dpfbz2, * epfbz2;
	int cbz2err, dbz2err, ebz2err;
	int fd;
	ssize_t oldsize,newsize;
	ssize_t bzctrllen,bzdatalen;
	u_char header[32],buf[8];
	u_char *old, *new;
	off_t oldpos,newpos;
	off_t ctrl[3];
	off_t lenread;
	off_t i;

	if(argc!=4) errx(1,"usage: %s oldfile newfile patchfile\n",argv[0]);

	/* Open patch file */
	if ((f = fopen(argv[3], "r")) == NULL)
		err(1, "fopen(%s)", argv[3]);

	/*
	File format:
		0	8	"BSDIFF40"
		8	8	X
		16	8	Y
		24	8	sizeof(newfile)
		32	X	bzip2(control block)
		32+X	Y	bzip2(diff block)
		32+X+Y	???	bzip2(extra block)
	with control block a set of triples (x,y,z) meaning "add x bytes
	from oldfile to x bytes from the diff block; copy y bytes from the
	extra block; seek forwards in oldfile by z bytes".
	*/

	/* Read header */
	if (fread(header, 1, 32, f) < 32) {
		if (feof(f))
			errx(1, "Corrupt patch\n");
		err(1, "fread(%s)", argv[3]);
	}

	/* Check for appropriate magic */
	if (memcmp(header, "BSDIFF40", 8) != 0)
		errx(1, "Corrupt patch\n");

	/* Read lengths from header */
	bzctrllen=offtin(header+8);
	bzdatalen=offtin(header+16);
	newsize=offtin(header+24);
	if((bzctrllen<0) || (bzdatalen<0) || (newsize<0))
		errx(1,"Corrupt patch\n");

	/* Close patch file and re-open it via libbzip2 at the right places */
	if (fclose(f))
		err(1, "fclose(%s)", argv[3]);
	if ((cpf = fopen(argv[3], "r")) == NULL)
		err(1, "fopen(%s)", argv[3]);
	if (fseeko(cpf, 32, SEEK_SET))
		err(1, "fseeko(%s, %lld)", argv[3],
		    (long long)32);
	if ((cpfbz2 = BZ2_bzReadOpen(&cbz2err, cpf, 0, 0, NULL, 0)) == NULL)
		errx(1, "BZ2_bzReadOpen, bz2err = %d", cbz2err);
	if ((dpf = fopen(argv[3], "r")) == NULL)
		err(1, "fopen(%s)", argv[3]);
	if (fseeko(dpf, 32 + bzctrllen, SEEK_SET))
		err(1, "fseeko(%s, %lld)", argv[3],
		    (long long)(32 + bzctrllen));
	if ((dpfbz2 = BZ2_bzReadOpen(&dbz2err, dpf, 0, 0, NULL, 0)) == NULL)
		errx(1, "BZ2_bzReadOpen, bz2err = %d", dbz2err);
	if ((epf = fopen(argv[3], "r")) == NULL)
		err(1, "fopen(%s)", argv[3]);
	if (fseeko(epf, 32 + bzctrllen + bzdatalen, SEEK_SET))
		err(1, "fseeko(%s, %lld)", argv[3],
		    (long long)(32 + bzctrllen + bzdatalen));
	if ((epfbz2 = BZ2_bzReadOpen(&ebz2err, epf, 0, 0, NULL, 0)) == NULL)
		errx(1, "BZ2_bzReadOpen, bz2err = %d", ebz2err);

	if(((fd=open(argv[1],O_RDONLY,0))<0) ||
		((oldsize=lseek(fd,0,SEEK_END))==-1) ||
		((old=malloc(oldsize+1))==NULL) ||
		(lseek(fd,0,SEEK_SET)!=0) ||
		(read(fd,old,oldsize)!=oldsize) ||
		(close(fd)==-1)) err(1,"%s",argv[1]);
	if((new=malloc(newsize+1))==NULL) err(1,NULL);

	oldpos=0;newpos=0;
	while(newpos<newsize) {
		/* Read control data */
		for(i=0;i<=2;i++) {
			lenread = BZ2_bzRead(&cbz2err, cpfbz2, buf, 8);
			if ((lenread < 8) || ((cbz2err != BZ_OK) &&
			    (cbz2err != BZ_STREAM_END)))
				errx(1, "Corrupt patch\n");
			ctrl[i]=offtin(buf);
		};

		/* Sanity-check */
		if(newpos+ctrl[0]>newsize)
			errx(1,"Corrupt patch\n");

		/* Read diff string */
		lenread = BZ2_bzRead(&dbz2err, dpfbz2, new + newpos, ctrl[0]);
		if ((lenread < ctrl[0]) ||
		    ((dbz2err != BZ_OK) && (dbz2err != BZ_STREAM_END)))
			errx(1, "Corrupt patch\n");

		/* Add old data to diff string */
		for(i=0;i<ctrl[0];i++)
			if((oldpos+i>=0) && (oldpos+i<oldsize))
				new[newpos+i]+=old[oldpos+i];

		/* Adjust pointers */
		newpos+=ctrl[0];
		oldpos+=ctrl[0];

		/* Sanity-check */
		if(newpos+ctrl[1]>newsize)
			errx(1,"Corrupt patch\n");

		/* Read extra string */
		lenread = BZ2_bzRead(&ebz2err, epfbz2, new + newpos, ctrl[1]);
		if ((lenread < ctrl[1]) ||
		    ((ebz2err != BZ_OK) && (ebz2err != BZ_STREAM_END)))
			errx(1, "Corrupt patch\n");

		/* Adjust pointers */
		newpos+=ctrl[1];
		oldpos+=ctrl[2];
	};

	/* Clean up the bzip2 reads */
	BZ2_bzReadClose(&cbz2err, cpfbz2);
	BZ2_bzReadClose(&dbz2err, dpfbz2);
	BZ2_bzReadClose(&ebz2err, epfbz2);
	if (fclose(cpf) || fclose(dpf) || fclose(epf))
		err(1, "fclose(%s)", argv[3]);

	/* Write the new file */
	if(((fd=open(argv[2],O_CREAT|O_TRUNC|O_WRONLY,0666))<0) ||
		(write(fd,new,newsize)!=newsize) || (close(fd)==-1))
		err(1,"%s",argv[2]);

	free(new);
	free(old);

	return 0;
}

// 合并方法的C实现
JNIEXPORT void JNICALL Java_com_hc_essay_library_util_PatchUtils_combine
		(JNIEnv *env, jclass jclz, jstring old_pak_path,
		 jstring new_apk_path, jstring patch_path){
	// 1.封装参数
	int argc = 4;
	char * argv[4];
	// 1.1 转换  jstring -> char*
	char* old_pak_cstr = (char*)(*env)->GetStringUTFChars(env,old_pak_path,NULL);
	char* new_apk_cstr = (char*)(*env)->GetStringUTFChars(env,new_apk_path,NULL);
	char* patch_cstr = (char*)(*env)->GetStringUTFChars(env,patch_path,NULL);
	// 第0的位置随便给
	argv[0] = "combine";
	argv[1] = old_pak_cstr;
	argv[2] = new_apk_cstr;
	argv[3] = patch_cstr;

	// 2.调用上面的方法  int argc,char * argv[]
	combine(argc,argv);

	// 3.释放资源
	(*env)->ReleaseStringUTFChars(env,old_pak_path,old_pak_cstr);
	(*env)->ReleaseStringUTFChars(env,new_apk_path,new_apk_cstr);
	(*env)->ReleaseStringUTFChars(env,patch_path,patch_cstr);
}