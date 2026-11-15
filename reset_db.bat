@echo off
cd /d "%~dp0"
echo Resetting MySQL database 'fastship' back to initial sample data (Table 41)...
docker exec -i fastship_mysql mysql -uroot < sql\schema.sql
docker exec -i fastship_mysql mysql -uroot fastship < sql\sample_data.sql
if %errorlevel% equ 0 (
    echo ============================================================================
    echo DONE! Database 'fastship' has been reset cleanly for a new Demo run!
    echo ============================================================================
) else (
    echo Failed to reset database. Make sure start_mysql.bat has been run first.
)
pause
