#=== FUNCTION ===================================================
#          NAME	: getSecret	
#   DESCRIPTION	: reads a secret filethat should be kept private
#   PARAMETER 1	: secret to get, should be the name prefix of the secret file to read
#	OUTCOME		: returns the content of the secret file, for instance, a password, 
#					as in: 
#						mysecret=$(getSecret secretPrefix)
#================================================================

getSecret()
{	
	local SECRET_SUFFIX=".SECRET"
	if [ -z $1 ] 
	then
		echo "!!! no secret to inspect !!! ...leaving."
		return 1
	fi
	secret="$1"
	folder=$(dirname $(readlink -f $0))
	secretfile="$folder/$secret$SECRET_SUFFIX"
	#echo "secret file is $secretfile"
	if [ ! -e $secretfile ]
	then
		echo "!!! no secret file [$secretfile] !!! ...leaving."
		return -1
	fi
	result=`cat $secretfile`
	echo "$result"
}
