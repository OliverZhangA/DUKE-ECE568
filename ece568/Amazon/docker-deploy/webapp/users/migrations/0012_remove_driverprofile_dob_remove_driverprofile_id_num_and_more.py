# Generated by Django 4.0.1 on 2022-04-23 21:00

from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('users', '0011_alter_driverprofile_image'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='driverprofile',
            name='DOB',
        ),
        migrations.RemoveField(
            model_name='driverprofile',
            name='ID_num',
        ),
        migrations.RemoveField(
            model_name='driverprofile',
            name='license_num',
        ),
        migrations.RemoveField(
            model_name='driverprofile',
            name='name',
        ),
        migrations.RemoveField(
            model_name='driverprofile',
            name='plate_num',
        ),
        migrations.RemoveField(
            model_name='driverprofile',
            name='special_info',
        ),
        migrations.RemoveField(
            model_name='driverprofile',
            name='vehicle_capacity',
        ),
        migrations.RemoveField(
            model_name='driverprofile',
            name='vehicle_type',
        ),
    ]
